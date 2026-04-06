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

import beast.base.evolution.tree.Node;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeIntervals;
import beast.base.util.HeapSort;

import java.util.List;

public class GRTIntervals extends TreeIntervals {

    protected boolean isGRT = false;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        isGRT = (treeInput.get() instanceof GermlineRootTree && ((GermlineRootTree) treeInput.get()).getGermlineNum() > 0);
    }

    /**
     * extract coalescent times and tip information into array times from beast.tree.
     *
     * @param mrca        the node representing the mrca
     * @param times       the times of the nodes in the beast.tree
     * @param childCounts the number of children of each node
     */
    protected static void collectGRTTimes(Node mrca, double[] times, int[] childCounts, int[] indices) {
        if (!(mrca instanceof GRTNode)) {
            throw new IllegalArgumentException("Germline Root Tree not properly configured");
        }
        List<Node> nodes = ((GRTNode) mrca).getSubtreeNodesAsArray();

        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            times[i] = node.getHeight();
            childCounts[i] = node.isLeaf() ? 0 : 2;
            indices[i] = node.getNr();
        }
    }

    public int getTotalCoalescentEvents() {
        Tree tree = treeInput.get();
        int allEvents = tree.getInternalNodeCount();
        if (tree instanceof GermlineRootTree) {
            if (((GermlineRootTree) tree).getGermlineNum() > 0) {
                return allEvents - 1;
            }
        }
        return allEvents;
    }

    protected void calculateGRTIntervals(GermlineRootTree tree) {
        // let's get the proper mrca:
        Node root = tree.getRoot();
        Node mrca;
        Node left = root.getLeft();
        Node right = root.getRight();
        if (left.getID() != null && left.getID().toUpperCase().contains("germline".toUpperCase())) {
            mrca = right;
        } else if (right.getID() != null && right.getID().toUpperCase().contains("germline".toUpperCase())) {
            mrca = left;
        } else {
            if (tree.getGermlineNum() > 0) {
                throw new IllegalArgumentException("Germline Root Tree not properly configured.");
            }
            mrca = root;
//            System.out.println("we're here");
//            super.calculateIntervals();
//            return;
//            throw new IllegalArgumentException("Germline Root Tree not properly configured.");
        }

        final int nodeCount = mrca.getNodeCount();

        times = new double[nodeCount];
        int[] childCounts = new int[nodeCount];

        int[] treeIndices = new int[nodeCount];

        collectGRTTimes(mrca, times, childCounts, treeIndices);

        indices = new int[nodeCount];

        HeapSort.sort(times, indices);

        if (intervals == null || intervals.length != nodeCount) {
            intervals = new double[nodeCount];
            lineageCounts = new int[nodeCount];
            lineagesAdded = new List[nodeCount];
            lineagesRemoved = new List[nodeCount];
//            lineages = new List[nodeCount];

            storedIntervals = new double[nodeCount];
            storedLineageCounts = new int[nodeCount];

        } else {
            for (List<Node> l : lineagesAdded) {
                if (l != null) {
                    l.clear();
                }
            }
            for (List<Node> l : lineagesRemoved) {
                if (l != null) {
                    l.clear();
                }
            }
        }

        // start is the time of the first tip
        double start = times[indices[0]];
        int numLines = 0;
        int nodeNo = 0;
        intervalCount = 0;
        while (nodeNo < nodeCount) {

            int lineagesRemoved = 0;
            int lineagesAdded = 0;

            double finish = times[indices[nodeNo]];
            double next;

            do {
                final int childIndex = indices[nodeNo];
                final int childTreeIndex = treeIndices[childIndex];
                final int childCount = childCounts[childIndex];
                // don't use nodeNo from here on in do loop
                nodeNo += 1;
                if (childCount == 0) {
                    addLineage(intervalCount, tree.getNode(childTreeIndex));
                    lineagesAdded += 1;
                } else {
                    lineagesRemoved += (childCount - 1);

                    // record removed lineages
                    final Node parent = tree.getNode(childTreeIndex);
                    //assert childCounts[indices[nodeNo]] == beast.tree.getChildCount(parent);
                    //for (int j = 0; j < lineagesRemoved + 1; j++) {
                    for (int j = 0; j < childCount; j++) {
                        Node child = j == 0 ? parent.getLeft() : parent.getRight();
                        removeLineage(intervalCount, child);
                    }

                    // record added lineages
                    addLineage(intervalCount, parent);
                    // no mix of removed lineages when 0 th
                    if (multifurcationLimit == 0.0) {
                        break;
                    }
                }

                if (nodeNo < nodeCount) {
                    next = times[indices[nodeNo]];
                } else break;
            } while (Math.abs(next - finish) <= multifurcationLimit);

            if (lineagesAdded > 0) {

                if (intervalCount > 0 || ((finish - start) > multifurcationLimit)) {
                    intervals[intervalCount] = finish - start;
                    lineageCounts[intervalCount] = numLines;
                    intervalCount += 1;
                }

                start = finish;
            }

            // add sample event
            numLines += lineagesAdded;

            if (lineagesRemoved > 0) {

                intervals[intervalCount] = finish - start;
                lineageCounts[intervalCount] = numLines;
                intervalCount += 1;
                start = finish;
            }
            // coalescent event
            numLines -= lineagesRemoved;
        }

        intervalsKnown = true;

    }

    @Override
    /**
     * Recalculates all the intervals for the given beast.tree.
     */
    @SuppressWarnings("unchecked")
    protected void calculateIntervals() {
        Tree tree = treeInput.get();

        if (tree instanceof GermlineRootTree && ((GermlineRootTree) tree).getGermlineNum() > 0) {
//            do the GRTIntervals instead
            calculateGRTIntervals((GermlineRootTree) tree);
        }
        else {
            super.calculateIntervals();
        }

    }

    @Override
    protected void restore() {
        super.restore();
        //intervalsKnown = false;
        intervalsKnown = false;
////        double[] tmp = storedIntervals;
//        storedIntervals = intervals;
////        intervals = tmp;
//
//        int[] tmp2 = storedLineageCounts;
////        storedLineageCounts = lineageCounts;
//        lineageCounts = tmp2;
//
//        int tmp3 = storedIntervalCount;
////        storedIntervalCount = intervalCount;
//        intervalCount = tmp3;
    }

    @Override
    public int getSampleCount() {
        // Assumes a binary tree!
        if (treeInput.get() instanceof GermlineRootTree && ((GermlineRootTree) treeInput.get()).getGermlineNum() > 0) {
            return treeInput.get().getInternalNodeCount() - 1;
        }
        return treeInput.get().getInternalNodeCount();
    }

    @Override
    public double[] getCoalescentTimes(double[] coalescentTimes) {
        if (!(treeInput.get() instanceof GermlineRootTree) || ((GermlineRootTree) treeInput.get()).getGermlineNum() < 0) {
            return super.getCoalescentTimes(coalescentTimes);
        }

        if (intervals.length != intervalCount && intervalsKnown) {
            System.out.println("Before recalc, intervals: intervals.length = " + intervals.length + " intervalCount = " + intervalCount);
        }

        if (!intervalsKnown || intervals.length != intervalCount) {
            calculateIntervals();
        }

        if (intervals.length != intervalCount) {
            System.out.println("After, intervals: intervals.length = " + intervals.length + " intervalCount = " + intervalCount);
        }

        if (coalescentTimes == null) coalescentTimes = new double[getSampleCount()];

        double time = 0;
        int coalescentIndex = 0;
        for (int i = 0; i < intervals.length; i++) {

            time += intervals[i];
            for (int j = 0; j < getCoalescentEvents(i); j++) {
                coalescentTimes[coalescentIndex] = time;
                coalescentIndex += 1;
            }
        }
        return coalescentTimes;
    }
}
