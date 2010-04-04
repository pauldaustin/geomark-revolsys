package com.revolsys.parallel.process;

import com.revolsys.parallel.channel.Channel;
import com.revolsys.parallel.channel.ClosedException;

public final class Delta<T> extends AbstractInOutProcess<T> {

  /** The second output Channel<T> */
  private Channel<T> out2;

  public Delta() {
  }

  /**
   * Construct a new Delta process with the input Channel<T> in and the output
   * Channel<T>s out1 and out2. The ordering of the Channel<T>s out1 and out2
   * make no difference to the functionality of this process.
   * 
   * @param in The input channel
   * @param out1 The first output Channel<T>
   * @param out2 The second output Channel<T>
   */
  public Delta(Channel<T> in, Channel<T> out, Channel<T> out2) {
    super(in, out);
    this.out2 = out2;
  }

  protected void run(Channel<T> in, Channel<T> out) {
    try {
      while (true) {
        T value = in.read();
        // TODO should write in parallel
        out.write(value);
        out2.write(value);
      }
    } catch (ClosedException e) {
    } catch (ThreadDeath e) {
    } finally {
      if (out2 != null) {
        out2.writeDisconnect();
      }
    }

  }

  /**
   * @return the out
   */
  public Channel<T> getOut2() {
    if (out2 == null) {
      setOut2(new Channel<T>());
    }
    return out2;
  }

  /**
   * @param out the out to set
   */
  public void setOut2(Channel<T> out2) {
    this.out2 = out2;
    out2.writeConnect();

  }

  public void setOut2Process(final InProcess<T> out2) {
    setOut2(out2.getIn());
  }

  public InProcess<T> getOut2Process() {
    return null;
  }
}
