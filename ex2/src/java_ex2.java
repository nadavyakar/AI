import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//class MinMax {
//    public ??? search(String[][] input_matrix) {
//
//    }
//}

public class java_ex2 {
//        protected static void produce_output(String output_file_path, Response response) throws IOException {
//            Writer writer = null;
//            try {
//                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output_file_path)));
//                if (response.has_reached_goal) {
//                    StringBuilder sb = new StringBuilder();
//                    for (Direction d : response.way_to_goal) {
//                        sb.append(d+"-");
//                    }
//                    sb.deleteCharAt(sb.length()-1);
//                    sb.append(" "+response.way_cost);
//                    writer.write(sb.toString());
//                } else {
//                    writer.write("no path");
//                }
//            } finally {
//                try {writer.close();} catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
//        }

    protected static String[][] parse_input(String input_file_path) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(input_file_path));
            List<String[]> game_board_lines_list = new ArrayList<String[]>();
            String line = br.readLine();
            while (line != null) {
                game_board_lines_list.add(line.split(""));
                line = br.readLine();
            }
            return Arrays.copyOf(game_board_lines_list.toArray(),
                    game_board_lines_list.size(), String[][].class);
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
            for (String[] l:parse_input("/home/nadav/workspaces/ml/ex2/resources/input.txt")) {
                for (String s:l) {
                    System.out.print(s);
                }
                System.out.println();
            }
//                produce_output("output.txt", new MinMax().search(parse_input("input.txt")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}