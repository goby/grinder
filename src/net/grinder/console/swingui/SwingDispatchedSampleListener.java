// The Grinder
// Copyright (C) 2000, 2001  Paco Gomez
// Copyright (C) 2000, 2001  Philip Aston

// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.grinder.console.swingui;

import java.util.Set;
import javax.swing.SwingUtilities;

import net.grinder.console.model.SampleListener;
import net.grinder.statistics.CumulativeStatistics;
import net.grinder.statistics.IntervalStatistics;


/**
 * SampleListener Decorator that disptaches the reset() and update()
 * notifications via a Swing thread.
 *
 * @author Philip Aston
 * @version $Revision$
 */
class SwingDispatchedSampleListener implements SampleListener
{
    private final SampleListener m_delegate;

    public SwingDispatchedSampleListener(SampleListener delegate)
    {
	m_delegate = delegate;
    }

    public void update(final IntervalStatistics intervalStatistics,
		       final CumulativeStatistics cumulativeStatistics)
    {
	SwingUtilities.invokeLater(
	    new Runnable() {
		public void run() {
		    m_delegate.update(intervalStatistics,
				      cumulativeStatistics);
		}
	    }
	    );
    }
}
