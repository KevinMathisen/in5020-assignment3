package protocol;


import crypto.ConsistentHashing;
import p2p.NetworkInterface;


import java.util.*;
import p2p.NodeInterface;

/**
 * This class implements the chord protocol. The protocol is tested using the custom built simulator.
 */
public class ChordProtocol implements Protocol{

    // length of the identifier that is used for consistent hashing
    public int m;

    // network object
    public NetworkInterface network;

    // consisent hasing object
    public ConsistentHashing ch;

    // key indexes. tuples of (<key name>, <key index>)
    public HashMap<String, Integer> keyIndexes;


    public ChordProtocol(int m){
        this.m = m;
        setHashFunction();
        this.keyIndexes = new HashMap<String, Integer>();
    }



    /**
     * sets the hash function
     */
    public void setHashFunction(){
        this.ch = new ConsistentHashing(this.m);
    }

  

    /**
     * sets the network
     * @param network the network object
     */
    public void setNetwork(NetworkInterface network){
        this.network = network;
    }


    /**
     * sets the key indexes. Those key indexes can be used to  test the lookup operation.
     * @param keyIndexes - indexes of keys
     */
    public void setKeys(HashMap<String, Integer> keyIndexes){
        this.keyIndexes = keyIndexes;
    }



    /**
     *
     * @return the network object
     */
    public NetworkInterface getNetwork(){
        return this.network;
    }






    /**
     * This method builds the overlay network.  It assumes the network object has already been set. It generates indexes
     *     for all the nodes in the network. Based on the indexes it constructs the ring and places nodes on the ring.
     *         algorithm:
     *           1) for each node:
     *           2)     find neighbor based on consistent hash (neighbor should be next to the current node in the ring)
     *           3)     add neighbor to the peer (uses Peer.addNeighbor() method)
     */
    public void buildOverlayNetwork(){

        /*
        implement this logic
         */

    }






    /**
     * This method builds the finger table. The finger table is the routing table used in the chord protocol to perform
     * lookup operations. The finger table stores m-entries. Each ith entry points to the ith finger of the node.
     * Each ith entry stores the information of it's neighbor that is responsible for indexes ((n+2^i-1) mod 2^m).
     * i = 1,...,m.
     *
     *Each finger table entry should consists of
     *     1) start value - (n+2^i-1) mod 2^m. i = 1,...,m
     *     2) interval - [finger[i].start, finger[i+1].start)
     *     3) node - first node in the ring that is responsible for indexes in the interval
     */
    public void buildFingerTable() {
        // Retrieve all nodes
        LinkedHashMap<String, NodeInterface> nodes = network.getTopology();

        // Create the finger table for each node
        for (NodeInterface node : nodes.values()) {
            int nodeIndex = node.getId();                               // NB: Assume that this is set in buildOverlayNetwork()

            // Initialize finger table
            List<Map<String, Object>> fingerTable = new ArrayList<>();

            // Create m amount of entries
            for (int i = 1; i <= m; i++) {
                Map<String, Object> entry = new HashMap<>();

                // Calculate start
                int start = (int) (nodeIndex + Math.pow(2, (i-1))) % (int) Math.pow(2, m);

                // Calculate end of interval
                int end = (int) (nodeIndex + Math.pow(2, (i))) % (int) Math.pow(2, m);
                end = (i == m) ? end : end-1; // Last entry should be the first value, so do not subtract 1

                // Find the successor node for the interval
                // This is the first successor which has an index higher or equal to the start value
                NodeInterface successorNode = node.getSuccessor();      // NB: Assume that neighbors are set in buildOverlayNetwork()
                while (successorNode.getId() < start) successorNode = successorNode.getSuccessor();


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
     *  Given the key index, it starts with one of the node in the network and follows through the finger table.
     *  The correct successors would be identified and the request would be checked in their finger tables successively.
     *   Finally the request will reach the node that contains the data item.
     *
     * @param keyIndex index of the key
     * @return names of nodes that have been searched and the final node that contains the key
     */
    public LookUpResponse lookUp(int keyIndex){
        /*
        implement this logic
         */
        return null;
    }



}
