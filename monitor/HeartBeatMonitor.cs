using Docker.DotNet;
using Docker.DotNet.Models;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

public class HeartBeatMonitor
{
    private readonly DockerClient _dockerClient;
    private readonly Dictionary<string, ServiceState> _serviceStates;
    private readonly ServiceFilter _serviceFilters;

    public HeartBeatMonitor()
    {
        string dockerUri = GetDockerUri();
        _dockerClient = new DockerClientConfiguration(new Uri(dockerUri)).CreateClient();

        _serviceFilters = new ServiceFilter
        {
            Name = new[] { "mystack_spring-boot-app", "mystack_nginx-rtmp" }
        };

        _serviceStates = new Dictionary<string, ServiceState>();
    }

    //get the docker uri based on the user's system
    private string GetDockerUri()
    {
        if (Environment.OSVersion.Platform == PlatformID.Win32NT)
        {
            return "npipe://./pipe/docker_engine";
        }

        return "unix:///var/run/docker.sock";
    }

    public async Task MonitorAsync()
    {
        Console.WriteLine("Connecting to Docker...");
        Console.WriteLine("Monitoring services: \n- mystack_spring-boot-app\n- mystack_nginx-rtmp");

        var services = await _dockerClient.Swarm.ListServicesAsync(new ServicesListParameters
        {
            Filters = _serviceFilters
        });

        Console.WriteLine($"Services matched: {services.Count()}");

        foreach (var service in services)
        {
            Console.WriteLine($"\n🔧 Service: {service.Spec.Name}");

            string updateState = service.UpdateStatus?.State?.ToString() ?? "unknown";

            _serviceStates[service.Spec.Name] = new ServiceState
            {
                State = updateState,
                LastUpdated = DateTime.UtcNow
            };

            Console.WriteLine($"Last Updated: {_serviceStates[service.Spec.Name].LastUpdated}");

            var tasks = await _dockerClient.Tasks.ListAsync(new TasksListParameters
            {
                Filters = new Dictionary<string, IDictionary<string, bool>>
                {
                    { "service", new Dictionary<string, bool> { { service.ID, true } } }
                }
            });

            ulong totalReplicas = service.Spec.Mode?.Replicated?.Replicas ?? 0;

            Console.WriteLine($"Desired Replicas: {totalReplicas}");
            Console.WriteLine($"Total Tasks Found: {tasks.Count}");

            // Group tasks by state
            var groupedTasks = tasks
                .GroupBy(t => t.Status?.State.ToString().ToLower() ?? "unknown")
                .OrderBy(g => g.Key);

            Dictionary<string, int> stateCounts = new();
            foreach (var group in groupedTasks)
            {
                string stateName = group.Key.ToUpper();
                int count = group.Count();
                stateCounts[stateName] = count;

                if(stateName == "COMPLETE") {
                    Console.WriteLine($"\n  ▶️ {stateName}: {count}");

                } else if(stateName == "FAILED"){
                    Console.WriteLine($"\n  ❌ {stateName}: {count}");
                } else if(stateName == "RUNNING") {
                    Console.WriteLine($"\n  ✅ {stateName}: {count}");
                } else {
                    Console.WriteLine($"\n ℹ️ {stateName}: {count}");
                }


                foreach (var task in group)
                {
                    string containerId = task.Status?.ContainerStatus?.ContainerID ?? "n/a";
                    string message = task.Status?.Err ?? "No error";
                    string node = task.NodeID ?? "unknown-node";
                    string shortId = task.ID.Substring(0, 12);

                    Console.WriteLine($"    - TaskID: {shortId} | Container: {containerId} | Node: {node} | Message: {message}");
                }
            }

            int running = stateCounts.TryGetValue("RUNNING", out var val) ? val : 0;
            string serviceHealthStatus;

            if ((ulong)running == totalReplicas)
            {
                Console.ForegroundColor = ConsoleColor.Green;
                serviceHealthStatus = "✅ Healthy - All replicas are running";
                Console.WriteLine($"Service Health Status: {serviceHealthStatus}");
                Console.ResetColor();
            }
            else if ((ulong)running < totalReplicas)
            {
                ulong diff = totalReplicas - (ulong)running;
                Console.ForegroundColor = ConsoleColor.Yellow;
                serviceHealthStatus = $"⚠️ Degrade - {running} out of {totalReplicas} replicas are running...spinning up {diff} more replica(s)";
                Console.WriteLine($"Service Health Status: {serviceHealthStatus}");
                Console.ResetColor();
            }

            else if(running == 0)
            {
                Console.ForegroundColor = ConsoleColor.Red;
                serviceHealthStatus = "❌ Unhealthy - No replicas are running";
                Console.WriteLine($"Service Health Status: {serviceHealthStatus}");
                Console.ResetColor();
            }

            
        }
    }
}
