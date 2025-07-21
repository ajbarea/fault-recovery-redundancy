Choas choasEngineering = new Choas();

string[] services = { "mystack_spring-boot-app", "mystack_nginx-rtmp" };

//continously randomly select a service to apply choas engineering
while (true)
{
    //random select a service
    Random rand = new Random();

    string serviceName = services[rand.Next(services.Length)];
    Console.WriteLine($"Applying choas engineering on service: {serviceName}");

    //get all containers for the selected services
    var serviceContainers = await choasEngineering.GetContainersByServiceNameAsync(serviceName);

    if (serviceContainers.Count == 0)
    {
        Console.WriteLine($"No running containers found for service: {serviceName}");
        continue;
    }

    //randomly apply choas engineering on each of the containers
    foreach (var container in serviceContainers)
    {

        //check if the container is running
        if (container.State.ToLower() != "running")
        {
            Console.WriteLine($"Container {container.ID} is not running. Skipping choas engineering.");
            continue;
        }

        //randomly select a choas engineering action
        int maxAction = 6;

        //randomly select an action
        int action = rand.Next(maxAction);

        switch (action)
        {
            case 0:
                // stop the container (kill)
                Console.WriteLine($"Stopping container: {container.ID}");
                await choasEngineering.StopContainer(container.ID);
                break;

            case 1:
                //stopping the container's network
                Console.WriteLine($"Stopping container network: {container.ID}");
                await choasEngineering.StopContainerNetwork(container.ID);
                break;
            case 2:
                // stress the cpu
                int cpu = rand.Next(1, 5); // random CPU count between 1 and 4
                int seconds = rand.Next(5, 16); // random duration between 5 and 15 seconds
                Console.WriteLine($"Running CPU stress on container: {container.ID} with {cpu} CPUs for {seconds} seconds");
                await choasEngineering.RunStressCPUAsync(container.ID, cpu, seconds);
                break;

            case 3:
                // stress the memory
                int totalVMB = rand.Next(1, 5); // random total VMB between 1 and 4
                int memorySizeMB = rand.Next(100, 501); // random memory size between 100MB and 500MB
                seconds = rand.Next(5, 16); // random duration between 5 and 15 seconds
                Console.WriteLine($"Running Memory stress on container: {container.ID} with {totalVMB} VMs of {memorySizeMB}MB for {seconds} seconds");
                await choasEngineering.RunStressMemoryAsync(container.ID, totalVMB, memorySizeMB, seconds);
                break;

            case 4:
                //introducing delay on the network
                int delaySeconds = rand.Next(10, 30); // random delay between 10 and 30 seconds
                Console.WriteLine($"Introducing network delay on container: {container.ID} for {delaySeconds} seconds");
                await choasEngineering.AddNetworkDelayAsync(container.ID, delaySeconds);
                break;
            case 5:
                //run all stressors
                int totalInstances = rand.Next(3, 5);
                //random between seconds and minutes
                //if 1 do second else minute
                string timeout = rand.Next(1, 3) == 1 ? $"{rand.Next(5, 16)}s" : $"{rand.Next(1, 6)}m";
                Console.WriteLine($"Running all stressors on container: {container.ID} with {totalInstances} instances for {timeout}");
                await choasEngineering.RunAllStressAsync(container.ID, totalInstances, timeout);
                break;
                //more later?

        }

        // wait for a random time before applying the next choas engineering
        int waitTime = rand.Next(5, 16); // random wait time between 5 and 15 seconds
        Console.WriteLine($"Waiting for {waitTime} seconds before applying next choas engineering");
        await Task.Delay(waitTime * 1000);
    }
}
