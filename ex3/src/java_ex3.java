interface ClusteringAlgorithm {

}
class SingleLinkClustering implements ClusteringAlgorithm {

}
class AvergeLinkClustering implements ClusteringAlgorithm {

}

public class java_ex33 {
    protected static void produce_output(String output_file_path, List<Integer> cluster_affiliation) throws IOException {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output_file_path)));
            for (int cluster : cluster_affiliation) {
                writer.write(cluster);
            }
        } finally {
            try {
                writer.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected static ClusteringAlgorithm parse_input(String input_file_path) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(input_file_path));
            ClusteringAlgorithm clustering_algorithm =
                    br.readLine().equals("single link")?new SingleLinkClustering():AvergeLinkClustering();
            clustering_algorithm.set_cluster_amount(Integer.valueOf(br.readLine()));
            String line = br.readLine();
            while (line != null) {
                clustering_algorithm.coordinates.append(java.util.StringTokenizer(line);
                line = br.readLine();
            }
            return clustering_algorithm;
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String argv[]) {
        try {
            produce_output("output.txt", parse_input("input.txt").calc‬_‫‪clusters());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}