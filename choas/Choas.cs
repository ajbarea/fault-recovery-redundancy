using System.Data;
using System.Text;
using Docker.DotNet;
using Docker.DotNet.Models;


public class Choas
{
    private string _dockerUri;
    private DockerClient _dockerClient;

    public Choas()
    {
        _dockerUri = this.GetDockerUri();
        _dockerClient = new DockerClientConfiguration(new Uri(_dockerUri)).CreateClient();
    }

    //get the docker uri based on the user's system
    private string GetDockerUri()
    {
        //we assume linux / mac
        string uri = "unix:///var/run/docker.sock";

        if (Environment.OSVersion.Platform == PlatformID.Win32NT)
        {
            //for windows, we use the named pipe
            uri = "npipe://./pipe/docker_engine";
        }

        return uri;
    }

    public async Task<IList<ContainerListResponse>> GetContainersByServiceNameAsync(string serviceName)
    {
        // Get all containers
        var containers = await _dockerClient.Containers.ListContainersAsync(new ContainersListParameters() { All = true });

        // Filter containers by service name and running
        var filteredContainers = containers
            .Where(c => c.Names.Any(n => n.Contains(serviceName)) && c.State.ToLower() == "running")
            .ToList();
        //var filteredContainers = containers.Where(c => c.Names.Any(n => n.Contains(serviceName))).ToList();

        return filteredContainers;
    }
    public async Task ExecInContainerAsync(string containerId, string command)
    {
        string[] commandArray = command.Split(' ');
        await ExecInContainerAsync(containerId, commandArray);
    }



    public async Task AddNetworkDelayAsync(string containerId, int seconds)
    {
        //convert seconds to milliseconds
        int milliseconds = seconds * 1000;
        //add network delay using tc command
        string command = $"tc qdisc add dev eth0 root netem delay {milliseconds}ms";
        await ExecInContainerAsync(containerId, command);
    }

    public async Task RunStressCPUAsync(string containerId, int cpuCount, int seconds)
    {
        // Run stress command to consume CPU
        string command = $"stress-ng --cpu {cpuCount} --timeout {seconds}";
        await ExecInContainerAsync(containerId, command);
    }

    public async Task RunStressMemoryAsync(string containerId, int totalVMB, int memorySizeMB, int seconds)
    {
        // Run stress command to consume memory
        string command = $"stress-ng --vm {totalVMB} --vm-bytes {memorySizeMB}M --timeout {seconds}";
        await ExecInContainerAsync(containerId, command);
    }


    public async Task RunAllStressAsync(string containerId, int totalInstances, string timeout)
    {
        string command = $"stress-ng --all {totalInstances} --timeout {timeout}";
        await ExecInContainerAsync(containerId, command);
    }



    public async Task ExecInContainerAsync(string containerId, string[] command)
    {
        var execCreateResponse = await _dockerClient.Exec.ExecCreateContainerAsync(containerId, new ContainerExecCreateParameters
        {
            AttachStdout = true,
            AttachStderr = true,
            Cmd = command,
            Privileged = true,
            Tty = false
        });

        using var stream = await _dockerClient.Exec.StartAndAttachContainerExecAsync(execCreateResponse.ID, false);

        var output = new MemoryStream();
        var buffer = new byte[4096];

        while (true)
        {
            var result = await stream.ReadOutputAsync(buffer, 0, buffer.Length, CancellationToken.None);
            if (result.EOF) break;

            output.Write(buffer, 0, result.Count);
        }

        string res = Encoding.UTF8.GetString(output.ToArray());

        Console.WriteLine("Result:" + res);
    }

    public Task<bool> StopContainer(string containerId)
    {
        try
        {
            // Stop the container
            _dockerClient.Containers.StopContainerAsync(containerId, new ContainerStopParameters()).Wait();
            Console.WriteLine($"Container {containerId} stopped successfully.");
            return Task.FromResult(true);

        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error stopping container {containerId}: {ex.Message}");
            return Task.FromResult(false);
        }
    }

    public async Task<bool> StopContainerNetwork(string containerId)
    {
        try
        {
            // Stop the container network
            var container = await _dockerClient.Containers.InspectContainerAsync(containerId);
            if (container.NetworkSettings.Networks == null || !container.NetworkSettings.Networks.Any())
            {
                Console.WriteLine($"Container {containerId} has no networks to disconnect.");
                return false;

            }

            foreach (var network in container.NetworkSettings.Networks.Keys)
            {
                await _dockerClient.Networks.DisconnectNetworkAsync(network, new NetworkDisconnectParameters
                {
                    Container = containerId,
                    Force = true
                });
            }

            Console.WriteLine($"Container {containerId} network stopped successfully.");
            return true;
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error stopping container {containerId} network: {ex.Message}");
            return false;
        }
    }
}



