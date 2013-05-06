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
package net.dawnfirerealms.legends.library.database

import net.dawnfirerealms.legends.library.race.Race

/**
 * @author B2OJustin
 */
class YAMLDataSourceTest extends GroovyTestCase {
    YAMLDataSource yamlDataSource;

    void setUp() {
        yamlDataSource = new YAMLDataSource("src/main/resources/");
    }

    void testLoadLPlayer() {
    }

    void testLoadRace() {
        Race testRace = new Race();
        ArrayList<String> testRaceDescription = new ArrayList<>();
        testRaceDescription.add("Test Race");
        testRace.setName("Test");
        testRace.setDescription(testRaceDescription);

        Race race = yamlDataSource.loadRace("Test");
        assertEquals(testRace.getName(), race.getName());
        assertEquals(testRace.getDescription(), race.getDescription());
    }

    void testLoadLClass() {
    }
}
