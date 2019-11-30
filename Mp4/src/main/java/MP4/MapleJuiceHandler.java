package MP4;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

public class MapleJuiceHandler extends Thread
{
    private static final String Map = null;
    private ConcurrentLinkedQueue<Job> jobs;
    private ConcurrentLinkedQueue<Task> tasks;
    private CopyOnWriteArraySet<String> runningWorkers;
    private CopyOnWriteArraySet<String> runningJobs;
    private ConcurrentHashMap<String, Integer> jobsToFinishedTasks;
    private static GrepLogger logger = GrepLogger.getInstance();
    private static TcpClientModule client = new TcpClientModule();

    public MapleJuiceHandler()
    {
        this.runningWorkers = new CopyOnWriteArraySet<String>();
        this.runningJobs = new CopyOnWriteArraySet<String>();
        this.jobsToFinishedTasks = new ConcurrentHashMap<String, Integer>();
    }

    /**
     * TODO : Handle intermediate file deletions incase of worker failure during task.
     * TODO : Handle task timeouts.
     */
    @Override
    public void run()
    {
        while(true)
        {
            this.jobs = MapleJuiceList.getJobs();
            this.tasks = MapleJuiceList.getTasks();
            try
            {
                for (Task task : this.tasks) 
                {
                    List<MembershipNode> nodes = MembershipList.getMembershipNodes();
                    if (task.status.equals(TaskStatus.FINISHED))
                    {
                        logger.LogInfo("[MapleJuiceHandler] Removing task from the list with Id: " + task.taskId);
                        this.runningWorkers.remove(task.workerIp);
                        if (jobsToFinishedTasks.contains(task.exeFileName))
                        {
                            int oldValue = jobsToFinishedTasks.get(task.exeFileName);
                            jobsToFinishedTasks.replace(task.exeFileName, oldValue, oldValue + 1);
                        }
                        else
                        {
                            jobsToFinishedTasks.put(task.exeFileName, 1);
                        }

                        MapleJuiceList.checkJobCompletion(task.exeFileName, jobsToFinishedTasks.get(task.exeFileName));
                        continue; 
                    }

                    if(task.status.equals(TaskStatus.STARTED))
                    {
                        boolean nodeIsAlive = false;
                        for (MembershipNode node : nodes) 
                        {
                            if (node.ipAddress.equals(task.workerIp))
                            {
                                nodeIsAlive = true;
                            }   
                        }

                        if (!nodeIsAlive)
                        {
                            // TODO : Handle to delete intermediate files of this worker role.
                            MapleJuiceList.changeTaskStatus(task.taskId, TaskStatus.NOTSTARTED);
                            this.runningWorkers.remove(task.workerIp);
                            continue;
                        }
                    }

                    if (task.status.equals(TaskStatus.NOTSTARTED) && !runningWorkers.contains(task.workerIp))
                    {
                        boolean nodeIsAlive = false;
                        for (MembershipNode node : nodes) 
                        {
                            if (node.ipAddress.equals(task.workerIp))
                            {
                                nodeIsAlive = true;
                            }   
                        }

                        boolean canSubmitJob = false;
                        if(nodeIsAlive)
                        {
                            canSubmitJob = true;
                        }
                        else
                        {
                            this.runningWorkers.remove(task.workerIp);
                            List<String> assignedWorkers = MapleJuiceList.getJobsToWorkerPool(task.exeFileName);
                            if (!assignedWorkers.contains(task.workerIp))
                            {
                                String newWorkerIp = assignedWorkers.get(0);
                                MapleJuiceList.updateTaskWorkerIp(task.taskId, newWorkerIp);
                                task.workerIp = newWorkerIp;
                                if (!runningWorkers.contains(task.workerIp))
                                {
                                    canSubmitJob = true;
                                }
                            }
                            else
                            {
                                assignedWorkers.remove(task.workerIp);
                                String newWorkerIp = getnewWorkerIp(assignedWorkers, nodes);
                                if (!newWorkerIp.isEmpty())
                                {
                                    assignedWorkers.add(newWorkerIp);
                                    newWorkerIp = assignedWorkers.get(0);
                                }
                                MapleJuiceList.addJobToWorkerPool(task.exeFileName, assignedWorkers);
                                if (!runningWorkers.contains(task.workerIp))
                                {
                                    canSubmitJob = true;
                                }
                            }
                        }

                        if (canSubmitJob)
                        {
                            task.submit();
                            runningWorkers.add(task.workerIp);
                            MapleJuiceList.changeTaskStatus(task.taskId, TaskStatus.STARTED);
                            if (!runningJobs.contains(task.exeFileName))
                            {
                                MapleJuiceList.changeJobStatus(task.exeFileName, TaskStatus.STARTED);
                                runningJobs.add(task.exeFileName);
                            }
                        }
                    }
                }

                Thread.sleep(5000);
            }
            catch(Exception e)
            {

            }
        }
    }

    private String getnewWorkerIp(List<String> assignedWorkers, List<MembershipNode> nodes) 
    {
        String newWorkerIp = "";
        for (MembershipNode node : nodes) 
        {
            if (!assignedWorkers.contains(node.ipAddress))
            {
                newWorkerIp = node.ipAddress;
            }
        }

        return newWorkerIp;
    }
}