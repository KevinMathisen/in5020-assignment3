

import p2p.Network;
import p2p.NetworkInterface;
import p2p.NodeInterface;
import crypto.ConsistentHashing;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import protocol.ChordProtocol;
import protocol.LookUpResponse;
import protocol.Protocol;

import java.util.*;

/**
 * This class simulates the chord protocol.
 */
public class ChordProtocolSimulator {
    // define chordProtocol object
    private ChordProtocol chordProtocol;

    // constants
    // length of the identifier used in consistent hasing
    public int m;
    // number of nodes in the network
    public int nodeCount;
    // number of keys used in the chord protocol
    public int keyCount;

    // each key is indexed using consistent hashing. eg:- the tuple (key 1, 123) represents a key with a name
    //'key 1' and the index value of 123. Each key is assumed to have a unique name.
    public LinkedHashMap<String, Integer> keyIndexes;

    // protocol denotes the protocol object that is used for simulation.
    public Protocol protocol;

    // network denotes the network object that contains the network of nodes
    public NetworkInterface network;

    // deontes the object of the consistent hashing that is used in hash calculation
    public ConsistentHashing consistentHash;



    public ChordProtocolSimulator(Protocol protocol, Network network, int m, int nodeCount, int keyCount){
        this.keyIndexes = new LinkedHashMap<>();
        this.protocol = protocol;
        this.network = network;
        this.consistentHash = new ConsistentHashing(m);
        this.m = m;
        this.nodeCount = nodeCount;
        this.keyCount  = keyCount;
        this.chordProtocol = new ChordProtocol(m);
    }

    /*
    This method creates new object of the current class

    @param protocol - it is the object of the protocol (eg:- chord protocol)
    @param network - it is the object of the network of nodes
    @return - retuns the object of the chord protocol simulator class
     */

    /**
     * This method creates a new object of the chord protocol simulator.
     *
     * @param network - the network object
     * @param m - 'm' value used in consistent hashing
     * @param keyCount - number of keys that needs to be assigned to nodes
     * @return the chord protocol simulator object
     */
    public static ChordProtocolSimulator getInstance(Network network, int m, int keyCount){

        Protocol chordProtocol = new ChordProtocol(m);
        int nodeCount = network.getSize();
        ChordProtocolSimulator chordProtocolSimulator = new ChordProtocolSimulator(chordProtocol, network, m, nodeCount,
                keyCount);
        return chordProtocolSimulator;
    }



    /**
     * This method assign indexes of keys to nodes in the network.
     *
     *     First keys are generated randomly. Each key is assigned a key name and it's store in the keyIndexes. Then key
     *     indexes are added to the node.
     *
     *     For each key index:
     *         1) find the node that should be responsible for the key (the index of the node should be greater than the
     *         index of key and the key index should be close to node index in the consistent hash ring)
     *         2) add the key index to the node (by calling the peer.add_data() from the network)
     */
    public void assignKeys(){

        generateKeys();
        for(Map.Entry<String, Integer> entry: keyIndexes.entrySet())
        {
//            String keyName =  entry.getKey();
            int keyIndex =  entry.getValue();
            String peerName = findPeer(keyIndex);
//           System.out.println("key name: "+name +"\t key_index : "+key_index+"\t peer_name : "+peer_name);
            protocol.getNetwork().getTopology().get(peerName).addData(keyIndex);
        }
    }



    /**
     * This method generates 'KeyCount' number of keys. each key has name "key"+i i=1...KeyCount
     *     For each key:
     *         1) generate index using consistent hashing. The index is m-bit length.
     *         consistentHashing.hash("key name") is used to calculate the index
     *         2) store the ("key name", "index") in the keyIndexes hash map
     */
    public  void generateKeys(){

        for(int i=1;i<keyCount+1;i++)
        {
            String keyName =  "key "+i;
            int keyIndex = consistentHash.hash(keyName);
            this.keyIndexes.put(keyName, keyIndex);
        }
    }



    /**
     * This method finds peer that should be responsible for a given key.
     * It retrieves the set of nodes from the network and for each node calculates the index of the node using
     * consistent hashing. Then it compares the index of the key with index of all the nodes. It choose a node
     * that has an index that would be next to the key index in the consistent hash ring.
     *
     * @param key_index index of the key
     * @return the name of the node that should be responsible for the key
     */
    public String findPeer(int key_index){

        List<Integer> sortedIndexes = new ArrayList<Integer>();
        int peerIndex = -1;

        // calculate the indexes of all the nodes using consistent hasing
        for(Map.Entry<String, NodeInterface> node: this.network.getTopology().entrySet()) {
            String nodeName = node.getValue().getName();
            int nodeIndex = consistentHash.hash(nodeName);
            sortedIndexes.add(nodeIndex);
        }

        // sort the node indexes
        Collections.sort(sortedIndexes);

        // check if the key index is larger than the biggest node index
        // if it is larger then the key should be placed before the lowest node index (at the start of the ring)
        if(key_index> sortedIndexes.get(sortedIndexes.size()-1)){
            peerIndex = sortedIndexes.get(0);
        }
        else{
            // if the key index is not larger than the biggest node index, then choose the node that has index
            // bigger than the key index. loop through the nodes in the ascending order of the node indexes
            for(int i=0;i < sortedIndexes.size(); i++){
                if(sortedIndexes.get(i) >= key_index){
                    peerIndex = sortedIndexes.get(i);
                    break;
                }
            }
        }

        // if no node is found then return null. Note: If everything works well then this shouldn't happen.
        if (peerIndex==-1){
            return null;
        }
        // return the name of the node that corresponds to the index
        return findPeerName(peerIndex);
    }


    /**
     * This method returns the peer name based on the peer index
     * This method uses a hack that the network setup uses the names Peer 1, Peer 2,... etc. It would be nice if this is
     * automatic. like fetching all the nodes from the network and compare the names with the given node (based on
     * index). Maybe I can improve it later.
     *
     * @param peerIndex index of the node
     * @return  name of the node
     */
    public String findPeerName(int peerIndex){


        int peerCount = nodeCount;
        for(int i=1; i<peerCount+1; i++){
            String name = "Node "+i;
            int index = consistentHash.hash(name);
            if(peerIndex==index){
                return name;
            }

        }
        return null;
    }



    /**
     *  This method prints the network. The network consists of set of nodes. It prints different information contained
     *  in the node such as neighbors, routing table, data.
     */
    public void printNetwork(){

        // gets the network from the protocol
        NetworkInterface network = protocol.getNetwork();

        // prints the top0logy
        network.printTopology();
    }


    /**
     * This method prints the ring overlay that chord protocol uses. It follows circular linked list algorithm to travel
     * the network. It starts with head and goes through successors. I am assuming that each peer stores only one
     * neighbor but i think the chord protocol doesn't make this restriction. but still even if  the node has multiple
     * neighbors this protocol should work.
     */
    public void printRing(){
        NodeInterface head = null;
        for(Map.Entry<String, NodeInterface> node: this.network.getTopology().entrySet()){
            head = node.getValue();
            break;
        }
        System.out.println("........printing ring..............");

        if(head.getNeighbors().size()==0){
            return;
        }

        System.out.print(head.getName());
        NodeInterface next = head.getSuccessor();
        while(true){
            System.out.print(" --- "+next.getName());
            next  = next.getSuccessor();
            if(next.getName().equals(head.getName())){
                System.out.print(" --- "+next.getName()+"\n");
                break;
            }
        }
        System.out.println(".....................................");
    }



    /**
     * This method tests the functioning of the lookup. This is a simple evaluation. For each key index it calls the
     * lookup from the chord protocol and returns the node index. It then compares the node index with the correct node
     * index (check response) is used for the comparison.
     */
    public void testLookUp(){

        for(Map.Entry<String, Integer> entry: keyIndexes.entrySet())
        {

            // lookup the key index
            LookUpResponse response = protocol.lookUp(entry.getValue());

            if(response == null)
            {
                return;
            }

            System.out.println(response.toString());
            // check whether the returned node index is correct or not
            if(checkResponse(entry.getValue(),response.node_name)){
                System.out.println("lookup successful for "+entry.getKey());
            }
            else
            {
                System.out.println("lookup failed for "+entry.getKey());
                break;
            }
        }

    }


    /**
     * This method compares whether the node actually stores the given key index or not
     *  It retrieves the data items stored at the particular node and compares whether the key index is stored or not
     * @param keyIndex index of the key
     * @param peerName name of the node
     * @return true if the node stores the key index otherwise return false
     */
    public boolean checkResponse(int keyIndex, String peerName){
        LinkedHashSet<Integer> dataItems = (LinkedHashSet<Integer>) this.network.getNode(peerName).getData();
        for(Integer data : dataItems){
            if(data==keyIndex){
                return true;
            }
        }
        return false;
    }



    /**
     * This method builds the chord protocol.
    1) sets the network
    2) builds the overlay network
    3) builds the finger table
     */

    public void buildProtocol(){
        protocol.setNetwork(network);
        assignKeys();
        protocol.setKeys(keyIndexes);
        protocol.buildOverlayNetwork();
        protocol.buildFingerTable();
    }

    /**
     * This is the starting point of this protocol.
     * This method starts the simulation.
     *     1) builds the chord protocol
     *     2) generate keys and assign it to nodes
     *     3) tests the look up operation (only if necessary)
     *     4) logs output
     */
    public void start(int nodeCount, int m) {
        System.out.println("Starting Chord protocol simulation...");
    
        // Build protocol (overlay network and routing table)
        System.out.println("Building protocol...");
        buildProtocol();
        System.out.println("Protocol built successfully.");
    
        // Print network structure
        System.out.println("Printing ring...");
        printRing();
        System.out.println("Ring printed.");
    
        System.out.println("Printing network...");
        printNetwork();
        System.out.println("Network printed.");
    
        // Lookup all keys and log results
        System.out.println("Starting key lookups...");
        int totalHops = 0;
        List<String> lookupResults = new ArrayList<>();
    
        // iterates through each key and perfomr a lookup using chordProtocol.lookUp(keyIndex)
        for (Map.Entry<String, Integer> entry : chordProtocol.keyIndexes.entrySet()) {
            String keyName = entry.getKey();
            int keyIndex = entry.getValue();
        
            System.out.println("Looking up key: " + keyName + " with index: " + keyIndex);
            
            // Perform the lookup for the current key (response contains lookup path/route and responsible node)
            LookUpResponse response = chordProtocol.lookUp(keyIndex);
        
            System.out.println("Lookup completed for key: " + keyName + ", reached node: " + response.node_name);
            int hopCount = response.peers_looked_up.size(); 
            totalHops += hopCount; // Increment total hops for average calculation
    
            // Use the response's toString() method to format and save the lookup result
            lookupResults.add(response.toString());
        }
    
        // Calculate average hop count
        double avgHopCount = !lookupResults.isEmpty() ? (double) totalHops / lookupResults.size() : 0;
        System.out.println("Average hop count calculated: " + avgHopCount);
        
        // Print each lookup result
        lookupResults.forEach(System.out::println);
        System.out.println("average hop count = " + avgHopCount);
    
        // Save results to a file in the output directory
        outputToFile(lookupResults, avgHopCount, nodeCount, m);
    }
    
    private void outputToFile(List<String> lookupResults, double avgHopCount, int nodeCount, int m) {
        // Define the output file name based on node count and m value
        String fileName = "output/output_" + nodeCount + "_nodes_m" + m + ".txt";
        File outputDir = new File("output");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        try (PrintWriter writer = new PrintWriter(new File(fileName), "UTF-8")) {
            // Write each lookup result
            for (String result : lookupResults) {
                writer.println(result);
            }
            // Write the average hop count at the end
            writer.println("average hop count = " + avgHopCount);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
