/*
This file is part of Legends.

    Legends is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Legends is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Legends.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.github.legendsdev.legends.library.event.weapon;

import com.github.legendsdev.legends.library.lplayer.LPlayer;
import com.github.legendsdev.legends.library.weapon.Weapon;

/**
 * @author B2OJustin
 */
public class WeaponHitEvent extends WeaponEvent {
    public Object target;

    public WeaponHitEvent(Weapon weapon, LPlayer source, Object target) {
        super(weapon, source);
        this.target = target;
    }

    public LPlayer getSource() {
        return getLPlayer();
    }

    public Object getTarget() {
        return target;
    }
}
