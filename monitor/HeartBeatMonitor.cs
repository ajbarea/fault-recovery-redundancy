using Docker.DotNet;
using Docker.DotNet.Models;


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

            Console.WriteLine($"Update Status: {updateState}, Last Updated: {_serviceStates[service.Spec.Name].LastUpdated}");

            // Correct Filter Format
            var taskFilters = new Dictionary<string, IList<string>>
            {
                { "service", new List<string> { service.ID } }
            };

           var tasks = await _dockerClient.Tasks.ListAsync(new TasksListParameters
            {
                Filters = new Dictionary<string, IDictionary<string, bool>>
                {
                    { "service", new Dictionary<string, bool> { { service.ID, true } } }
                }
            });

            var taskStateGroups = tasks
            .GroupBy(t =>
            {
                if (t.Status == null)
                    return "unknown";

                return t.Status.State.ToString().ToLower();
            })
            .ToDictionary(g => g.Key, g => g.Count());

            var totalReplicas = service.Spec.Mode?.Replicated?.Replicas ?? 0;

            Console.WriteLine($"Desired Replicas: {totalReplicas}");
            Console.WriteLine($"Total Tasks Found: {tasks.Count}");

            foreach (var stateGroup in taskStateGroups)
            {
                Console.WriteLine($"  {stateGroup.Key.ToUpper()}: {stateGroup.Value}");
            }

            if (taskStateGroups.TryGetValue("running", out int running) && running < (int)totalReplicas)
            {
                Console.WriteLine($"WARNING: Only {running} of {totalReplicas} replicas are running");
            }
        }
    }
}

