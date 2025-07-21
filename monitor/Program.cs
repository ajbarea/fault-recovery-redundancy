HeartBeatMonitor monitor = new HeartBeatMonitor();
while (true)
{
    try
    {
        await monitor.MonitorAsync();
    }
    catch (Exception ex)
    {
        Console.WriteLine($"Error during monitoring: {ex.Message}");
    }

    // Wait for a while before the next check
    await Task.Delay(TimeSpan.FromSeconds(5));
}