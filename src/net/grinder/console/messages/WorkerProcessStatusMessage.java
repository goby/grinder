// Copyright (C) 2001, 2002 Dirk Feufel
// Copyright (C) 2001, 2002, 2003, 2004, 2005 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.messages;

import net.grinder.common.WorkerProcessStatus;
import net.grinder.communication.Message;


/**
 * Message for informing the console of worker process status.
 *
 * @author Dirk Feufel
 * @author Philip Aston
 * @version $Revision$
 */
public final class WorkerProcessStatusMessage
  implements Message, WorkerProcessStatus {

  private static final long serialVersionUID = -2073574340466531680L;

  private final String m_agentName;
  private final String m_identity;
  private final String m_name;
  private final short m_state;
  private final short m_totalNumberOfThreads;
  private final short m_numberOfRunningThreads;

  /**
   * Creates a new <code>WorkerProcessStatusMessage</code> instance.
   *
   * @param agentName The name of the parent agent process.
   * @param identity Process identity.
   * @param name Process name.
   * @param state The process state. See {@link
   * net.grinder.common.WorkerProcessStatus}.
   * @param totalThreads The total number of threads.
   * @param runningThreads The number of threads that are still running.
   */
  public WorkerProcessStatusMessage(String agentName,
                                    String identity,
                                    String name,
                                    short state,
                                    short runningThreads,
                                    short totalThreads) {
    m_agentName = agentName;
    m_identity = identity;
    m_name = name;
    m_state = state;
    m_numberOfRunningThreads = runningThreads;
    m_totalNumberOfThreads = totalThreads;
  }

  /**
   * Accessor for the name of the parent agent process.
   *
   * @return The agent name.
   */
  public String getAgentName() {
    return m_agentName;
  }

  /**
   * Accessor for the process identity.
   *
   * @return The process name.
   */
  public String getIdentity() {
    return m_identity;
  }

  /**
   * Accessor for the process name.
   *
   * @return The process name.
   */
  public String getName() {
    return m_name;
  }

  /**
   * Accessor for the process state.
   *
   * @return The process state.
   */
  public short getState() {
    return m_state;
  }

  /**
   * Accessor for the number of running threads for the process.
   *
   * @return The number of running threads.
   */
  public short getNumberOfRunningThreads() {
    return m_numberOfRunningThreads;
  }

  /**
   * Accessor for the maximum number of threads for the process.
   *
   * @return The maximum number of threads for the process.
   */
  public short getMaximumNumberOfThreads() {
    return m_totalNumberOfThreads;
  }
}
