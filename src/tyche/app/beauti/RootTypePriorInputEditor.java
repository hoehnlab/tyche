/*
 *  Copyright (C) 2025 Hoehn Lab, Dartmouth College
 *
 * This file is part of TyCHE.
 *
 * TyCHE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * TyCHE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with TyCHE.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package tyche.app.beauti;

import beastfx.app.inputeditor.BEASTObjectInputEditor;
import tyche.inference.distribution.RootTypePrior;

/**
 * InputEditor for RootTypePrior
 * 
 * @author Jessie Fielding
 */
public class RootTypePriorInputEditor extends BEASTObjectInputEditor {
    
    public RootTypePriorInputEditor() {
        super();
    }

    @Override
    public Class<?> type() {
        return RootTypePrior.class;
    }
}
