package protocol;

import crypto.ConsistentHashing;
import p2p.NetworkInterface;

import java.util.*;
import p2p.NodeInterface;

/**
 * This class implements the chord protocol. The protocol is tested using the
 * custom built simulator.
 */
public class ChordProtocol implements Protocol {

    // length of the identifier that is used for consistent hashing
    public int m;

    // network object
    public NetworkInterface network;

    // consisent hasing object
    public ConsistentHashing ch;

    // key indexes. tuples of (<key name>, <key index>)
    public HashMap<String, Integer> keyIndexes;

    public ChordProtocol(int m) {
        this.m = m;
        setHashFunction();
        this.keyIndexes = new HashMap<String, Integer>();
    }

    /**
     * sets the hash function
     */
    public void setHashFunction() {
        this.ch = new ConsistentHashing(this.m);
    }

    /**
     * sets the network
     * 
     * @param network the network object
     */
    public void setNetwork(NetworkInterface network) {
        this.network = network;
    }

    /**
     * sets the key indexes. Those key indexes can be used to test the lookup
     * operation.
     * 
     * @param keyIndexes - indexes of keys
     */
    public void setKeys(HashMap<String, Integer> keyIndexes) {
        this.keyIndexes = keyIndexes;
    }

    /**
     *
     * @return the network object
     */
    public NetworkInterface getNetwork() {
        return this.network;
    }

    /**
     * This method builds the overlay network. It assumes the network object has
     * already been set. It generates indexes
     * for all the nodes in the network. Based on the indexes it constructs the ring
     * and places nodes on the ring.
     * algorithm:
     * 1) for each node:
     * 2) find neighbor based on consistent hash (neighbor should be next to the
     * current node in the ring)
     * 3) add neighbor to the peer (uses Peer.addNeighbor() method)
     */
    public void buildOverlayNetwork() {
        // Retrieve all nodes in the network
        LinkedHashMap<String, NodeInterface> topology = network.getTopology();

        // Generate node indexes and store them in a list
        List<Map.Entry<String, Integer>> nodesWithIndexes = new ArrayList<>();
        for (String nodeName : topology.keySet()) {
            int index = ch.hash(nodeName); // Hash each node's name to get a unique index
            topology.get(nodeName).setId(index); // Set the index for each node
            nodesWithIndexes.add(new AbstractMap.SimpleEntry<>(nodeName, index));
        }

        // Sort nodes by their indexes to arrange them in ring order
        nodesWithIndexes.sort(Map.Entry.comparingByValue());

        // Assign neighbors to form ring
        int numNodes = nodesWithIndexes.size();
        for (int i = 0; i < numNodes; i++) {
            // Current node and its successor (next node in the ring)
            String currentNodeName = nodesWithIndexes.get(i).getKey();
            String nextNodeName = nodesWithIndexes.get((i + 1) % numNodes).getKey(); // Wraps around to form a ring

            // Get NodeInterface objects from topology
            NodeInterface currentNode = topology.get(currentNodeName);
            NodeInterface nextNode = topology.get(nextNodeName);

            // Add the next node as neighbor to the current node
            currentNode.addNeighbor(nextNodeName, nextNode);
        }
    }

    /**
     * This method builds the finger table. The finger table is the routing table
     * used in the chord protocol to perform
     * lookup operations. The finger table stores m-entries. Each ith entry points
     * to the ith finger of the node.
     * Each ith entry stores the information of it's neighbor that is responsible
     * for indexes ((n+2^i-1) mod 2^m).
     * i = 1,...,m.
     *
     * Each finger table entry should consists of
     * 1) start value - (n+2^i-1) mod 2^m. i = 1,...,m
     * 2) interval - [finger[i].start, finger[i+1].start)
     * 3) node - first node in the ring that is responsible for indexes in the
     * interval
     */
    public void buildFingerTable() {
        // Retrieve all nodes
        LinkedHashMap<String, NodeInterface> nodes = network.getTopology();

        // Create the finger table for each node
        for (NodeInterface node : nodes.values()) {
            int nodeIndex = node.getId();

            // Initialize finger table
            List<Map<String, Object>> fingerTable = new ArrayList<>();

            // Create m amount of entries
            for (int i = 1; i <= m; i++) {
                Map<String, Object> entry = new HashMap<>();

                // Calculate start
                int start = (int) (nodeIndex + Math.pow(2, (i - 1))) % (int) Math.pow(2, m);

                // Calculate end of interval
                int end = (int) (nodeIndex + Math.pow(2, (i))) % (int) Math.pow(2, m);
                end = (i == m) ? end : end - 1; // Last entry should be the first value, so do not subtract 1

                // Find the successor node for the interval
                // This is the first successor which is placed after the interval start in the
                // overlay network
                //
                // Because of the network wrapping, we can't only check if the successor has a
                // larger index than the start
                // as the successor can have a smaller index, but still be the first successor
                // in the interval, if it appears after wrapping
                // or the start can have a small index, if it appears after wrapping, where we
                // also want a successor which is placed after the wrapping
                //
                // Instead, we need to check if the successor is the first which appears after
                // the start.
                // To do this we can shift the interval and node indexes to range from the
                // current node's index until node's index + 2^(m)
                // e.g. We have m = 3 (8 possible indexes)
                // if we are creating the finger table for index 4, and a node is placed at
                // index 1, we pretend it is placed at index 9 (1+8)
                // if an interval we created starts at index 2, then we can pretend it is placed
                // at index 10 (2+8)
                //
                // This allows us to now directly check if a node's (adjusted) index is bigger
                // than an intervals (adjusted) start

                NodeInterface successorNode = node; // Add 2^(m) to start if it is placed before the node's index in the
                                                    // overlay network:
                int adjusted_start = (start <= nodeIndex) ? start : start + (int) Math.pow(2, m);
                int adjusted_index;

                do {
                    successorNode = successorNode.getSuccessor(); // Retreive the next successor in the ring toplogy
                    adjusted_index = successorNode.getId(); // Get the index of the successor
                    if (adjusted_index <= nodeIndex)
                        adjusted_index += (int) Math.pow(2, m); // If the successor is placed before the node's index,
                                                                // add 2^(m) to allow it to be compared with start

                } while (adjusted_index < adjusted_start); // Go to next successor if this one is not within the
                                                           // interval

                // Save the values to the entry
                entry.put("start", start);
                entry.put("interval_start", start);
                entry.put("interval_end", end);
                entry.put("successor_node", successorNode.getName());

                // Save the entry to the fingerTable
                fingerTable.add(entry);
            }

            // Save the finger table to the node
            node.setRoutingTable(fingerTable);
        }
    }

    /**
     * This method performs the lookup operation.
     * Given the key index, it starts with one of the node in the network and
     * follows through the finger table.
     * The correct successors would be identified and the request would be checked
     * in their finger tables successively.
     * Finally the request will reach the node that contains the data item.
     *
     * @param keyIndex index of the key
     * @return names of nodes that have been searched and the final node that
     *         contains the key
     */
    public LookUpResponse lookUp(int keyIndex) {
        /*
         * implement this logic
         * 
         * Notes kevin:
         * Use network.getTopology() and find the node with the lowest key index to
         * start from?
         * alternatively chose node with name 'Node 1', or do someting else.
         * only important that we use the same node to start the search from each time.
         * 
         * When using fingertable, look at its values from the buildFingerTable()
         * function
         */

        // Create a set to track which nodes' finger tables are examined during the
        // lookup.
        LinkedHashSet<String> peersLookedUp = new LinkedHashSet<>();

        // Start the lookup from a predefined node ('Node 1') to maintain consistency
        // across all lookups.
        NodeInterface startNode = network.getTopology().get("Node 1");
        NodeInterface currentNode = startNode;
        int hopCount = 0; // Initialize hop count to measure the efficiency of the lookup process.

        while (true) {
            peersLookedUp.add(currentNode.getName()); // Log the current node as visited.
            hopCount++;

            // Check if the current node contains the key.
            Set<Integer> nodeData = (Set<Integer>) currentNode.getData();
            if (nodeData.contains(keyIndex)) {
                // If the key is found, return the response with details of the current node.
                return new LookUpResponse(peersLookedUp, currentNode.getId(), currentNode.getName());
            }

            // If the key isn't found, use the node's finger table to determine the next
            // node to visit.
            List<Map<String, Object>> fingerTable = (List<Map<String, Object>>) currentNode.getRoutingTable();
            NodeInterface nextNode = currentNode;

            // Identify the most appropriate successor node from the finger table.
            for (Map<String, Object> entry : fingerTable) {
                int start = (int) entry.get("start");
                int end = (int) entry.get("interval_end");

                // Check if the key index falls within the interval of the current finger table
                // entry.
                if (keyIndex >= start && keyIndex <= end) {
                    String successorNodeName = (String) entry.get("successor_node");
                    nextNode = network.getNode(successorNodeName);
                    break;
                }
            }

            // Prevent infinite loops by stopping if the same node is reached again.
            if (nextNode.equals(currentNode)) {
                break;
            }

            // Proceed to the next node as determined by the finger table.
            currentNode = nextNode;
        }

        // Return the response detailing the final node reached if the key was not
        // found.
        return new LookUpResponse(peersLookedUp, currentNode.getId(), currentNode.getName());
    }

}
