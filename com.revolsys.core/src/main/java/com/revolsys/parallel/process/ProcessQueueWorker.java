package com.revolsys.parallel.process;

import org.apache.log4j.Logger;

import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.ClosedException;

public class ProcessQueueWorker extends Thread implements Process {
  private ProcessQueue queue;

  private Channel<Process> in;

  private Process process;

  public ProcessQueueWorker(
    final ProcessQueue queue) {
    this.queue = queue;
    this.in = queue.getProcessChannel();
    setDaemon(true);
  }

  public void run() {
    queue.addWorker(this);
    try {
      while (true) {
        process = in.read(queue.getMaxWorkerIdleTime());
        if (process == null) {
          return;
        } else {
          try {
            process.run();
          } catch (Exception e) {
            if (!(e instanceof InterruptedException)) {
              Class<? extends Process> processClass = process.getClass();
              Logger log = Logger.getLogger(processClass);
              log.error(e.getMessage(), e);
            }
          }
        }
        process = null;
      }
    } catch (ClosedException e) {
      return;
    } finally {
      queue.removeWorker(this);
    }
  }

  public Process getProcess() {
    return process;
  }

  public String getBeanName() {
    return getClass().getName();
  }

  public void setBeanName(
    String name) {
  }
}
