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
import tyche.app.beauti.ElementwisePriorInputEditor;
import tyche.app.beauti.RootTypePriorInputEditor;
import tyche.app.beauti.RootTypePriorProvider;
import tyche.inference.distribution.ElementwisePrior;
import tyche.inference.distribution.RootTypePrior;

import static org.junit.Assert.*;

/**
 * Unit tests for Tyche prior InputEditors and PriorProvider
 * 
 * @author Jessie Fielding
 */
public class TychePriorInputEditorTest {

    @Test
    public void testElementwisePriorInputEditorType() {
        ElementwisePriorInputEditor editor = new ElementwisePriorInputEditor();
        assertEquals("ElementwisePriorInputEditor should handle ElementwisePrior",
                ElementwisePrior.class, editor.type());
    }

    @Test
    public void testElementwisePriorInputEditorInstantiation() {
        ElementwisePriorInputEditor editor = new ElementwisePriorInputEditor();
        assertNotNull("ElementwisePriorInputEditor should instantiate", editor);
    }

    @Test
    public void testRootTypePriorInputEditorType() {
        RootTypePriorInputEditor editor = new RootTypePriorInputEditor();
        assertEquals("RootTypePriorInputEditor should handle RootTypePrior",
                RootTypePrior.class, editor.type());
    }

    @Test
    public void testRootTypePriorInputEditorInstantiation() {
        RootTypePriorInputEditor editor = new RootTypePriorInputEditor();
        assertNotNull("RootTypePriorInputEditor should instantiate", editor);
    }

    @Test
    public void testRootTypePriorProviderInstantiation() {
        RootTypePriorProvider provider = new RootTypePriorProvider();
        assertNotNull("RootTypePriorProvider should instantiate", provider);
    }

    @Test
    public void testRootTypePriorProviderDescription() {
        RootTypePriorProvider provider = new RootTypePriorProvider();
        String desc = provider.getDescription();
        assertNotNull("Description should not be null", desc);
        assertTrue("Description should contain 'Root'", desc.contains("Root"));
        assertTrue("Description should contain 'Prior'", desc.contains("Prior"));
    }

    @Test
    public void testRootTypePriorProviderCanProvide() {
        RootTypePriorProvider provider = new RootTypePriorProvider();
        // Should be able to provide a prior for a document (even null doc for this basic check)
        assertTrue("Should be able to provide prior", provider.canProvidePrior(null));
    }
}
