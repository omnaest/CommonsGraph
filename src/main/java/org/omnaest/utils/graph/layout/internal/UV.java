/*******************************************************************************
 * Copyright 2026 Danny Kunz
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.omnaest.utils.graph.layout.internal;

/**
 * A point in the engine's canonical (u,v) coordinate system, before the final direction-dependent mapping to real (x,y).
 * Deliberately not the public {@link org.omnaest.utils.graph.layout.domain.Point} type, to keep "canonical, pre-mapping" and
 * "real, post-mapping" coordinates from being confused with one another.
 *
 * @author omnaest
 */
final class UV
{
    private final double u;
    private final double v;

    UV(double u, double v)
    {
        this.u = u;
        this.v = v;
    }

    double getU()
    {
        return this.u;
    }

    double getV()
    {
        return this.v;
    }

}
