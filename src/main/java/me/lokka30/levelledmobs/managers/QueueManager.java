package me.lokka30.levelledmobs.managers;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.QueueItem;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class QueueManager {

    public QueueManager(final LevelledMobs main){
        this.main = main;
        this.queue = new LinkedBlockingQueue<>();
    }

    private final LevelledMobs main;
    private boolean isRunning;
    private boolean doThread;
    private final LinkedBlockingQueue<QueueItem> queue;

    public void start(){
        if (isRunning) return;
        doThread = true;
        isRunning = true;

        BukkitRunnable bgThread = new BukkitRunnable(){
            @Override
            public void run() {
                try {
                      main();
                }
                catch (InterruptedException ignored){
                    isRunning = false;
                }
                Utils.logger.info("Queue Manager has exited");
            }
        };

        bgThread.runTaskAsynchronously(main);
    }

    public void stop(){
        doThread = false;
    }

    public void addToQueue(final QueueItem item){
        this.queue.offer(item);
    }

    private void main() throws InterruptedException{
        Utils.logger.info("Starting Main thread");
        while (doThread) {

            QueueItem item = queue.poll(1000, TimeUnit.MILLISECONDS);
            if (item == null) continue;

            main.levelManager.entitySpawnListener.preprocessMob(item.lmEntity, item.event);
        }

        isRunning = false;
    }
}