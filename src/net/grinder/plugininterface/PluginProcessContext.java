// The Grinder
// Copyright (C) 2000  Paco Gomez
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

package net.grinder.plugininterface;

import net.grinder.common.FilenameFactory;
import net.grinder.common.GrinderException;
import net.grinder.common.GrinderProperties;
import net.grinder.common.Logger;
import net.grinder.statistics.StatisticsView;


/**
 * This class is used to share data between the Grinder and the 
 * plug-in.
 * 
 * @author Paco Gomez
 * @author Philip Aston
 * @version $Revision$
 */
public interface PluginProcessContext extends Logger
{    
    String getGrinderID();

    FilenameFactory getFilenameFactory();
    
    /**
     * Returns the parameters specified with "grinder.plugin.parameter="
     */
    GrinderProperties getPluginParameters();

    void registerStatisticsView(StatisticsView view)
	throws GrinderException;
}
