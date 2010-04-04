package com.revolsys.parallel.process;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

public abstract class AbstractResetableProcess extends AbstractProcess {
  private String status = "initialized";

  private boolean running = false;

  private boolean pause = false;

  private boolean reset = false;

  private boolean waitForExecutionToFinish = false;

  private Set<UUID> executions = new LinkedHashSet<UUID>();

  private long waitTime = 1000;

  public void run() {
    running = true;
    try {
      while (running) {
        status = "resetting";
        executions.clear();
        reset();
        reset = false;
        while (running && !reset) {
          status = "starting execution";
          if (pause || !execute()) {
            if (pause) {
              status = "paused";
            } else {
              status = "waiting";
            }
            synchronized (this) {
              try {
                wait(waitTime);
              } catch (InterruptedException e) {
              }
            }
          }
        }

        synchronized (executions) {
          while (waitForExecutionToFinish && !executions.isEmpty()) {
            waitOnExecutions();
          }
        }
      }
    } finally {
      running = false;
      status = "terminated";
    }
  }

  protected void waitOnExecutions() {
    synchronized (executions) {
      status = "waiting on executions";
      try {
        executions.wait(waitTime);
      } catch (InterruptedException e) {
      }
    }
  }

  protected abstract boolean execute();

  protected void reset() {
  }

  @ManagedAttribute
  public int getExecutionCount() {
    return executions.size();
  }

  protected UUID startExecution() {
    synchronized (executions) {
      UUID id = UUID.randomUUID();
      executions.add(id);
      executions.notifyAll();
      return id;
    }
  }

  protected void finishExecution(final UUID id) {
    synchronized (executions) {
      executions.remove(id);
      executions.notifyAll();
    }
  }

  @ManagedAttribute
  public String getStatus() {
    return status;
  }

  protected void setStatus(final String status) {
    this.status = status;
  }

  /**
   * The pause causes the scheduler to sleep until a soft or hard reset is
   * initiated.
   */
  @ManagedOperation
  public void pause() {
    pause = true;
  }

  /**
   * The hard reset causes the scheduler loop to restart ignoring all current
   * executing requests. Upon reset the counts of executing requests and the
   * status of all jobs will be updated to ensure consistency.
   */
  @ManagedOperation
  public void hardReset() {
    waitForExecutionToFinish = false;
    pause = false;
    reset = true;
  }

  /**
   * The soft reset causes the scheduler loop to restart after all current
   * executing requests have completed. Upon reset the counts of executing
   * requests and the status of all jobs will be updated to ensure consistency.
   */
  @ManagedOperation
  public void softReset() {
    waitForExecutionToFinish = true;
    pause = false;
    reset = true;
  }

  public long getWaitTime() {
    return waitTime;
  }

  public void setWaitTime(
    long waitTime) {
    this.waitTime = waitTime;
  }
  
}
