# IN5020 Assignment2 - Peer to Peer Protocol

This is an assignment in the course Distributed System IN5020 at UIO.

The project consists of a java program simulating a peer-to-peer protocol, where data is evenly distributed among nodes.
All nodes have routing tables, allowing the nodes to find any data on the network by routing the request to other nodes.
Hashing is used to place data on nodes, and to route requests to various nodes.

## Workload distribution

| **Functionality**           | **Group Member** |
| --------------------------- | ---------------- |
| Build Overlay Network       | Anju             |
| Build Finger Tables         | Kevin            |
| Look up Mechanism           | Henrik           |
| File output                 | Anju             |
| Simulator, Debugging, Misc. | Everyone         |

## Requirements

To run the project, you need the following requirements:

- Maven (If you want to compile)
- Java

## Starting the Simulation

To start the Simulation, do the following steps:
(TODO: update)

1. Compile the java files using `mvn clean package`
2. Run the following command for starting the simulation:
   ```sh
   java -cp "target\assignment3-1.0-SNAPSHOT.jar" Simulation <node count> <m>
   ```

### Command-line arguments

`<node count>`
Represents the number of nodes in the network. This value is used to create
the basic network with the specified number of nodes. The Chord Protocol builds the Ring
overlay network on top of the basic network.

`m`
Represents the length of identifiers (m-bit) used in the Chord Protocol. For example:
if m is 3 then the identifier value can range from 0 to 7.

## Viewing Output

The output of the simulation is written to a file placed in `output/`, where it is named based on the command line arguments.

## Theory

The following is explanations of how the following functions were implemented.

### Implementation of buildOverlayNetwork()

The purpose of this function is to create an overlay network with a ring topology.
This is achieved by sorting the nodes by their index, which is the order of the nodes in the ring topology. To allow for traversal across the network, each node saves the next node in the ring as their neighbor using the node's `addNeighbor()` method.

To handle wrapping of the ring topology, the last node in the ring gets the first node as their neighbor.

### Implementation of buildFingerTable()

The purpose of this function is to build a routing table for each of the nodes in the network.
This is achieved by creating entries for all nodes routing tables.
Each entry consists of its start point, the interval of indexes the entry should match to, and the name of the successor node. The values are calculated as follows:

#### **Start**

Start: `(node_index + 2^(entry_num-1)) mod 2^(m)`

#### **End**

End: `(node_index + 2^(entry_num)) mod 2^(m) - 1`
Where the last entry in the routing table will NOT subtract 1, as it should also cover the first entry in the table

#### **Successor Node**

The successor node is the first node when moving through the ring topology which is placed after the start value of the interval.
We then simply need to move through the ring topology, checking if each node's index is larger than the start value.

However, to handle wrapping of values, as the topology is ring based, we need to slightly modify the approach.
To ensure that we can still simply check if the node's index is larger than the start value, both of these values can be modified.
If either of them is smaller than the index of the node we are creating a routing table from, they are placed after wrapping around the ring topology. To keep the comparison fair, and pretend we do not really wrap, we can add 2^(m) to their values.

For example, imagine we are creating a routing table for node 500, have a potential successor node with the value 100, and m is 10, so there are 1024 possible indexes in the ring. The interval [1000, 200] should then use this potential successor node as their successor, as it is placed after the start index in the ring. Following our logic above, we can check if 100 is less than 500, and as it is, it is converted to 1124. Then the previous check works, checking if 1124 is larger than 1000.
The result is that the node with index 100 is chosen for the interval [1000, 200].

### Implementation of lookup()

The purpose of this function is to find a key with a given index in the network.
To achieve this, we start at an arbitrary node, in our case `Node 1`.
For each node we visit, including `Node 1`, which first checks if they contain the key.
If not, we use the finger table to find which successor node we will jump to to continue our search. Here we iterate through each entry, checking if the key is inside of the entries intervals.
To handle wrapping, we apply similar logic as buildFingerTable(). When looking at a node's finger table, we convert all values less than its index by adding 2^(m). We can then simply check if the key is inside of this adjusted interval.

At each node we save their name if the key was not found to a list, allowing us to know which path the lookup took. Furthermore, when the key is found, we return the lookup path, and the name and index of the node which contained the key.
