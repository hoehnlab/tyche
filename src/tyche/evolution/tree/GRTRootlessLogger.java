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

package tyche.evolution.tree;

import beast.base.core.BEASTObject;
import beast.base.core.Function;
import beast.base.evolution.TreeWithMetaDataLogger;
import beast.base.evolution.branchratemodel.BranchRateModel;
import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.inference.StateNode;
import beast.base.inference.parameter.Parameter;
import beast.base.inference.parameter.RealParameter;

import java.io.PrintStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

public class GRTRootlessLogger extends TreeWithMetaDataLogger {

    boolean someMetaDataNeedsLogging;
    boolean substitutions = false;

    private DecimalFormat df;
    private boolean sortTree;

    @Override
    public void initAndValidate() {
        int dp = decimalPlacesInput.get();
        if (dp < 0) {
            df = null;
        } else {
            // just new DecimalFormat("#.######") (with dp time '#' after the decimal)
            df = new DecimalFormat("#."+new String(new char[dp]).replace('\0', '#'));
            df.setRoundingMode(RoundingMode.HALF_UP);
        }

        if (parameterInput.get().size() == 0 && clockModelInput.get() == null) {
            someMetaDataNeedsLogging = false;
            return;
            //throw new IllegalArgumentException("At least one of the metadata and branchratemodel inputs must be defined");
        }
        someMetaDataNeedsLogging = true;
        // without substitution model, reporting substitutions == reporting branch lengths
        if (clockModelInput.get() != null) {
            substitutions = substitutionsInput.get();
        }

        // default is to sort the tree
        sortTree = sortTreeInput.get();
    }

    @Override
    public void log(long sample, PrintStream out) {
        // make sure we get the current version of the inputs
        Tree tree = (Tree) treeInput.get().getCurrent();
        List<Function> metadata = parameterInput.get();
        for (int i = 0; i < metadata.size(); i++) {
            if (metadata.get(i) instanceof StateNode) {
                metadata.set(i, ((StateNode) metadata.get(i)).getCurrent());
            }
        }
        BranchRateModel.Base branchRateModel = clockModelInput.get();
        // write out the log tree with meta data
        out.print("tree STATE_" + sample + " = ");

        if (sortTree) {
            tree.getRoot().sort();
        }

        Node root = tree.getRoot();
        Node mrca = root;
        if (root instanceof GRTNode) {
            Node left = root.getLeft();
            Node right = root.getRight();
            if (left.getID() != null && left.getID().toUpperCase().contains("germline".toUpperCase())) {
                mrca = right;
            } else if (right.getID() != null && right.getID().toUpperCase().contains("germline".toUpperCase())) {
                mrca = left;
            }
        }

        out.print(toNewick(mrca, metadata, branchRateModel));
        //out.print(tree.getRoot().toShortNewick(false));
        out.print(";");
    }

    String toNewick(Node node, List<Function> metadataList, BranchRateModel.Base branchRateModel) {
        StringBuffer buf = new StringBuffer();
        if (node.getLeft() != null) {
            buf.append("(");
            buf.append(toNewick(node.getLeft(), metadataList, branchRateModel));
            if (node.getRight() != null) {
                buf.append(',');
                buf.append(toNewick(node.getRight(), metadataList, branchRateModel));
            }
            buf.append(")");
        } else {
            buf.append(node.getNr() + 1);
        }
        StringBuffer buf2 = new StringBuffer();
        if (someMetaDataNeedsLogging) {
            buf2.append("[&");
            if (metadataList.size() > 0) {
                boolean needsComma = false;
                for (Function metadata : metadataList) {
                    if (metadata instanceof Parameter<?>) {
                        Parameter<?> p = (Parameter<?>) metadata;
                        int dim = p.getMinorDimension1();
                        if (p.getMinorDimension2() > node.getNr()) {
                            if (needsComma) {
                                buf2.append(",");
                            }
                            buf2.append(((BEASTObject) metadata).getID());
                            buf2.append('=');
                            if (dim > 1) {
                                buf2.append('{');
                                for (int i = 0; i < dim; i++) {
                                    if (metadata instanceof RealParameter) {
                                        RealParameter rp = (RealParameter) metadata;
                                        appendDouble(buf2, rp.getMatrixValue(node.getNr(), i));
                                    } else {
                                        buf2.append(p.getMatrixValue(node.getNr(), i));
                                    }
                                    if (i < dim - 1) {
                                        buf2.append(',');
                                    }
                                }
                                buf2.append('}');
                            } else {
                                if (metadata instanceof RealParameter) {
                                    RealParameter rp = (RealParameter) metadata;
                                    appendDouble(buf2, rp.getArrayValue(node.getNr()));
                                } else {
                                    buf2.append(metadata.getArrayValue(node.getNr()));
                                }
                            }
                            needsComma = true;
                        } else {

                        }
                    } else {
                        if (metadata.getDimension() > node.getNr()) {
                            if (needsComma) {
                                buf2.append(",");
                            }
                            buf2.append(((BEASTObject) metadata).getID());
                            buf2.append('=');
                            buf2.append(metadata.getArrayValue(node.getNr()));
                            needsComma = true;
                        }
                    }
                }
                if (buf2.length() > 2 && branchRateModel != null) {
                    buf2.append(",");
                }
            }
            if (branchRateModel != null) {
                buf2.append("rate=");
                appendDouble(buf2, branchRateModel.getRateForBranch(node));
            }
            buf2.append(']');
        }
        if (buf2.length() > 3) {
            buf.append(buf2.toString());
        }
        buf.append(":");
        if (substitutions) {
            appendDouble(buf, node.getLength() * branchRateModel.getRateForBranch(node));
        } else {
            appendDouble(buf, node.getLength());
        }
        return buf.toString();
    }

    /**
     * Appends a double to the given StringBuffer, formatting it using
     * the private DecimalFormat instance, if the input 'dp' has been
     * given a non-negative integer, otherwise just uses default
     * formatting.
     * @param buf
     * @param d
     */
    private void appendDouble(StringBuffer buf, double d) {
        if (df == null) {
            buf.append(d);
        } else {
            buf.append(df.format(d));
        }
    }
}
