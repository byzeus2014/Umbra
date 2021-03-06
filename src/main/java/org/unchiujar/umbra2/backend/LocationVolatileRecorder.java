/*******************************************************************************
 * This file is part of Umbra.
 *
 *     Umbra is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Umbra is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Umbra.  If not, see <http://www.gnu.org/licenses/>.
 *
 *     Copyright (c) 2011 Vasile Jureschi <vasile.jureschi@gmail.com>.
 *     All rights reserved. This program and the accompanying materials
 *     are made available under the terms of the GNU Public License v3.0
 *     which accompanies this distribution, and is available at
 *
 *    http://www.gnu.org/licenses/gpl-3.0.html
 *
 *     Contributors:
 *        Vasile Jureschi <vasile.jureschi@gmail.com> - initial API and implementation
 *        Yen-Liang, Shen - Simplified Chinese and Traditional Chinese translations
 ******************************************************************************/


package org.unchiujar.umbra2.backend;

import org.unchiujar.umbra2.location.ApproximateLocation;

import java.util.ArrayList;
import java.util.List;

public class LocationVolatileRecorder implements ExploredProvider {

    private List<ApproximateLocation> mLocations = new ArrayList<ApproximateLocation>();

    private static LocationVolatileRecorder mInstance;

    private LocationVolatileRecorder() {
        super();
    }

    public static LocationVolatileRecorder getInstance() {
        return (mInstance == null) ? mInstance = new LocationVolatileRecorder()
                : mInstance;
    }

    @Override
    public void deleteAll() {
        mLocations.clear();

    }

    @Override
    public synchronized long insert(ApproximateLocation location) {
        mLocations.add(location);
        return mLocations.size();
    }

    @Override
    public void insert(List<ApproximateLocation> locations) {
        throw new UnsupportedOperationException("Insert multiple not supported.");
    }

    @Override
    public List<ApproximateLocation> selectAll() {
        return mLocations;
    }

    @Override
    public List<ApproximateLocation> selectVisited(
            ApproximateLocation upperLeft, ApproximateLocation bottomRight) {
        ArrayList<ApproximateLocation> visited = new ArrayList<ApproximateLocation>();
        for (ApproximateLocation location : mLocations) {
            if (location.getLatitude() >= upperLeft.getLatitude()
                    && location.getLatitude() <= bottomRight.getLatitude()
                    && location.getLongitude() >= upperLeft.getLongitude()
                    && location.getLongitude() <= bottomRight.getLongitude()) {
                visited.add(location);
            }
        }
        return visited;
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

}
