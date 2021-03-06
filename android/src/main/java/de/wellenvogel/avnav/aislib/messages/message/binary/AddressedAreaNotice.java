/* Copyright (c) 2011 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.wellenvogel.avnav.aislib.messages.message.binary;

import de.wellenvogel.avnav.aislib.messages.binary.BinArray;
import de.wellenvogel.avnav.aislib.messages.binary.SixbitException;

/**
 * Addressed area notice ASM
 */
public class AddressedAreaNotice extends AreaNotice {

    public AddressedAreaNotice() {
        super(23);
    }

    public AddressedAreaNotice(BinArray binArray) throws SixbitException {
        super(23, binArray);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AddressedAreaNotice [");
        builder.append(super.toString());
        builder.append("]");
        return builder.toString();
    }

}
