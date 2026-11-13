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

package test.tyche.app.beauti;

import org.junit.Test;
import tyche.app.beauti.TycheClockModelInputEditor;
import tyche.evolution.branchratemodel.AbstractTycheTypeLinkedClockModel;

import static org.junit.Assert.*;

/**
 * Unit tests for Tyche InputEditor classes
 * 
 * @author Jessie Fielding
 */
public class TycheInputEditorTest {

    @Test
    public void testTycheClockModelInputEditorType() {
        TycheClockModelInputEditor editor = new TycheClockModelInputEditor();
        assertEquals("TycheClockModelInputEditor should handle AbstractTycheTypeLinkedClockModel",
                AbstractTycheTypeLinkedClockModel.class, editor.type());
    }

    @Test
    public void testTycheClockModelInputEditorInstantiation() {
        TycheClockModelInputEditor editor = new TycheClockModelInputEditor();
        assertNotNull("TycheClockModelInputEditor should instantiate without error", editor);
    }
}
