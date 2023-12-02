import networkx as nx
import sys
from networkx.drawing.nx_pydot import read_dot, write_dot
import re

def extract_response_codes(label):
    # Extract numerical values from the label
    response_codes = re.findall(r'\b\d+\b', label)
    return response_codes

def transform_dot_file(input_dot_file, output_dot_file):
    # Read DOT file into a NetworkX graph
    graph = read_dot(input_dot_file)

    # Create a dictionary to store labels for each state
    label_dict = {}

    # Iterate through edges and consolidate labels for self-loops
    for edge in graph.edges(data=True):
        source = edge[0]
        target = edge[1]
        label = edge[2].get('label', '')

        # Check if it's a self-loop and not the __start0 state
        if source == target and source != '__start0':
            # Combine labels for the same state
            if source in label_dict:
                label_dict[source].append(label)
            else:
                label_dict[source] = [label]

    # Create a new graph for the modified self-loops
    modified_graph = nx.MultiDiGraph()

    # Add all other edges to the new graph
    for edge in graph.edges(data=True):
        source = edge[0]
        target = edge[1]
        label = edge[2].get('label', '')
        
        # Exclude __start0 state and its related edges
        if source != target and source != '__start0' and target != '__start0':
            modified_graph.add_edge(source, target, label=label)

    # Add consolidated self-loops with combined labels to the new graph
    for state, labels_list in label_dict.items():
        combined_response_codes = set()
        for labels in labels_list:
            combined_response_codes.update(extract_response_codes(labels))
        combined_label = 'others /' + '{{{}}}'.format(','.join(combined_response_codes))
        modified_graph.add_edge(state, state, label=combined_label)

    # Write the modified graph to the output DOT file
    write_dot(modified_graph, output_dot_file)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python script.py <source.dot> <dest.dot>")
        sys.exit(1)
    input_dot_file = sys.argv[1]
    output_dot_file = sys.argv[2]

    transform_dot_file(input_dot_file, output_dot_file)
