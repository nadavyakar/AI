import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * base class for cluster
 * responsible of management of mapping between input point indices and the points coordinates
 * (unification, distance calc, etc.)
 */
abstract class Cluster {
    int cluster_num;
    List<Object[]> indices_to_points = new ArrayList<>();

    public Cluster(int index, double[] init_point_coordinates) {
        this.cluster_num = index;
        this.add_point(index, init_point_coordinates);
    }

    void add_point(int index, double[] coordinates) {
        this.indices_to_points.add(new Object[] {index, coordinates});
    }

    void union(Cluster cluster) {
        for (Object[] index_to_point : cluster.get_indices_to_points()) {
            this.add_point((int) index_to_point[0], (double[]) index_to_point[1]);
        }
    }

    public List<int[]> get_idx_to_cnum_list() {
        return this.get_indices_to_points().stream().map(
                (Object[] index_to_point) -> new int[] {(int) index_to_point[0], this.cluster_num}).collect(Collectors.toList());
    }

    public List<Object[]> get_indices_to_points() {
        return this.indices_to_points;
    }

    List<double[]> get_points() {
        return this.indices_to_points.stream().map(
                (Object[] index_to_point) -> (double[]) index_to_point[1]).collect(Collectors.toList());
    }
    abstract double calc_distance(Cluster other_cluster);

    /**
     * @return a list of unique permutations between all of this and the input other clusters points
     */
    Set<Set<double[]>> get_points_pairs_permutations(Cluster other_cluster) {
        Set<Set<double[]>> points_pairs_permutations = new LinkedHashSet<>();
        for (double[] local_point : this.get_points()) {
            for (double[] other_point : other_cluster.get_points()) {
                points_pairs_permutations.add(
                        new LinkedHashSet<double[]>(Arrays.asList(new double[][]{local_point, other_point})));

            }
        }
        return points_pairs_permutations;
    }

    static double distance(double[] point_1, double[] point_2) {
        return Math.sqrt(Math.pow(point_1[0]-point_2[0],2)+Math.pow(point_1[1]-point_2[1],2));
    }
}

/**
 * responsible of calculating cluster distance in the single link manner per eucalidean distance
 */
class SingleLinkCluster extends Cluster {
    public SingleLinkCluster(int index, double[] init_point_coordinates) {
        super(index, init_point_coordinates);
    }
    double calc_distance(Cluster other_cluster) {
        double min_distance = Double.POSITIVE_INFINITY;
        for (Set<double[]> pair_of_points : this.get_points_pairs_permutations(other_cluster)) {
            Iterator<double[]> pair_of_points_itr = pair_of_points.iterator();
            min_distance = Math.min(min_distance, distance(pair_of_points_itr.next(),pair_of_points_itr.next()));
        }
        return min_distance;
    }
}

/**
 * responsible of calculating cluster distance in the average link manner per eucalidean distance
 */
class AvergeLinkCluster extends Cluster {
    public AvergeLinkCluster(int index, double[] init_point_coordinates) {
        super(index, init_point_coordinates);
    }

    public double calc_distance(Cluster other_cluster) {
        double sum_distance = 0;
        for (Set<double[]> pair_of_points : this.get_points_pairs_permutations(other_cluster)) {
            Iterator<double[]> pair_of_points_itr = pair_of_points.iterator();
            sum_distance+=distance(pair_of_points_itr.next(),pair_of_points_itr.next());
        }
        return sum_distance / (this.indices_to_points.size() + other_cluster.indices_to_points.size());
    }
}

/**
 * responsible of iterativley going over existing clusters and unifying the two closest ones in each iteration,
 * until the number of desired clusters is reached
 */
class ClusteringAlgorithm {
    int cluster_amount;
    Set<Cluster> clusters;
    ClusteringAlgorithm(int cluster_amount, Set<Cluster> init_clusters) {
        this.cluster_amount = cluster_amount;
        this.clusters = init_clusters;
    }

    /**
     * @return a list of cluster numbers which represent the affiliation of each point with
     * the respective index in the input file to its cluster number
     */
    List<Integer> calc‬_‫‪clusters() {
        while (this.cluster_amount<this.clusters.size()) {
            this.union(clusters, this.find_closest_clusters(clusters));
        }
        List<int[]> idx_to_cnum_list = new ArrayList<int[]>();
        for (Cluster cluster:clusters) {
            idx_to_cnum_list.addAll(cluster.get_idx_to_cnum_list());
        }
        Collections.sort(idx_to_cnum_list,
                (int[] idx_to_cnum_1, int[] idx_to_cnum_2) -> idx_to_cnum_1[0]-idx_to_cnum_2[0]);
        return idx_to_cnum_list.stream().map((int[] idx_to_cnum)->idx_to_cnum[1]).collect(Collectors.toList());
    }

    /**
     * unifies the two input clusters
     */
    private void union(Set<Cluster> clusters, Cluster[] closest_clusters) {
        Cluster min_idx_cluster , max_idx_cluster;
        if (closest_clusters[0].cluster_num < closest_clusters[1].cluster_num) {
            min_idx_cluster = closest_clusters[0];
            max_idx_cluster = closest_clusters[1];
        } else {
            min_idx_cluster = closest_clusters[1];
            max_idx_cluster = closest_clusters[0];
        }
        min_idx_cluster.union(max_idx_cluster);
        clusters.remove(max_idx_cluster);
    }

    /**
     * @return the two closest clusters per the single/average distance calculation
     */
    protected Cluster[] find_closest_clusters(Set<Cluster> clusters){
        Cluster[] closest_clusters = null;
        double min_distance = Double.POSITIVE_INFINITY;
        for (Cluster cluster_1:clusters) {
            for (Cluster cluster_2: clusters) {
                if (!cluster_1.equals(cluster_2)) {
                    double curr_clusters_distance = cluster_1.calc_distance(cluster_2);
                    if (curr_clusters_distance<min_distance) {
                        min_distance = curr_clusters_distance;
                        closest_clusters=new Cluster[] {cluster_1, cluster_2};
                    }
                }
            }
        }
        return closest_clusters;
    }

    void ClusteringAlgorithm(int cluster_amount) {
        this.cluster_amount=cluster_amount;
    }
}

public class java_ex3 {
    protected static void produce_output(String output_file_path, List<Integer> cluster_numbers) throws IOException {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output_file_path)));
            StringBuilder sb = new StringBuilder();
            // reformat cluster numbers for uniform display among students:
            Map<Integer, Integer> cluster_numbers_conv_map = new HashMap<Integer, Integer>();
            int reformatted_cluster_num = 0;
            for (int cluster_num : cluster_numbers) {
                if (!cluster_numbers_conv_map.containsKey(cluster_num)) {
                    cluster_numbers_conv_map.put(cluster_num, ++reformatted_cluster_num);
                }
            }
            for (int cluster_num : cluster_numbers) {
                sb.append(String.valueOf(cluster_numbers_conv_map.get(cluster_num))+"\n");
            }
            sb.delete(sb.length()-1, sb.length());
            writer.write(sb.toString());
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
            boolean is_single_link = br.readLine().equals("single link");
            int clusters_amount = Integer.valueOf(br.readLine());
            Set<Cluster> clusters = new LinkedHashSet<Cluster>();
            StringTokenizer st;
            String line = br.readLine();
            int i=0;
            while (line != null) {
                st = new StringTokenizer(line,",");
                clusters.add(is_single_link
                        ?new SingleLinkCluster(++i, new double[] {Double.valueOf(st.nextToken()), Double.valueOf(st.nextToken())})
                        :new AvergeLinkCluster(++i, new double[] {Double.valueOf(st.nextToken()), Double.valueOf(st.nextToken())}));
                line = br.readLine();
            }
            return new ClusteringAlgorithm(clusters_amount, clusters);
        } finally {
            try {
                if (br!=null) {
                    br.close();
                }
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