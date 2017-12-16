import java.io.*;
import java.util.*;

class Log {
    static Writer writer = null;
    public static void log(String msg) {
        try {
            if (writer==null) {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("/tmp/ml_ex2.log")));
            }
            System.out.println(msg);
            writer.write(msg+"\n");
        } catch (Exception e) {
            try {writer.close();} catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    static void close() {
        try {writer.close();} catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

class Choise {
    Choise(State state, Double propagated_value) {
        this.state = state;
        this.propagated_value = propagated_value;
    }
    // the state picked to be the next mokve by the search algorithm
    State state = null;
    // the gain in picking state state
    Double propagated_value = Double.NaN;
}

class Player {
    boolean black_player = true;
    String soldier;
    protected static HashMap<Object, Player> players = null;
    static HashMap<Object, Player> get_players() {
        if (players==null) {
            players = new HashMap<Object, Player>();
            players.put(true, new Player(true));
            players.put(false, new Player(false));
            players.put("B", new Player(true));
            players.put("W", new Player(false));
        }
        return players;
    }
    Player(boolean black_player) {
        this.black_player = black_player;
        this.soldier = black_player?"B":"W";
    }
    
    Choise choose(Choise choise_this_layer, Choise choise_next_layer) {
        Double choise_this_layer_val = choise_this_layer.propagated_value ==Double.NaN?Double.valueOf(0):choise_this_layer.propagated_value;
        Double choise_next_layer_val = choise_next_layer.propagated_value ==Double.NaN?Double.valueOf(0):choise_next_layer.propagated_value;
        if (choise_this_layer.state==null) {
            return choise_next_layer;
        }
        if (black_player) {
            return choise_this_layer_val < choise_next_layer_val?choise_next_layer:choise_this_layer;
        } else {
            return choise_this_layer_val > choise_next_layer_val?choise_next_layer:choise_this_layer;
        }
    }
}

class State {
    protected String[][] game_board;
    protected Set<int[]> open_cells_coordinates = new LinkedHashSet<int[]>();
    public Player player;
    public Double value;

    /**
     *
     * @param game_board
     * @param player the player for which this state is the initial state of one of his turns
     */
    public State(String[][] game_board, Player player) {
        this.game_board = game_board;
        this.value = eval_board();
        this.player=player;
    }

    protected Double eval_board() {
        // *_num represent the num of black/white/empty respectivley,
        // and black_border_num is the num of blacks on the boarded lines/columns
        Double black_num = 0.0, white_num = 0.0, empty_num =0.0, black_boarder_num=0.0;
        for (int i=0; i< this.game_board.length; i++) {
            for (int j=0; j< this.game_board[i].length; j++) {
                switch (this.game_board[i][j]) {
                    case "B":
                        black_num++;
                        if (i==0 || j==0 || i==this.game_board.length || j==this.game_board[i].length) {
                            black_boarder_num++;
                        }
                        break;
                    case "W":
                        white_num++;
                        break;
                    case "E":
                        empty_num++;
                        this.open_cells_coordinates.add(new int[] {i,j});
                        break;
                    default:
                        throw new RuntimeException("illegal game board cell");
                }
            }
        }
        // remove all empty cells not near any existing soldier
        for (int[] open_cell_coordinates:this.open_cells_coordinates) {
            int x=open_cell_coordinates[0], y=open_cell_coordinates[1];
            boolean found_neighbor_soldier = false;
            for (int i=-1; i<=1 && !found_neighbor_soldier; i++) {
                for (int j=-1; j<=1 && !found_neighbor_soldier; j++) {
                    if (x+i<0 || x+i > this.game_board.length || y+j<0 || y+j>this.game_board[0].length) {
                        continue;
                    }
                    if (!this.game_board[x+i][y+j].equals("E")) {
                        found_neighbor_soldier=true;
                    }
                }
            }
            if (!found_neighbor_soldier) {
                this.open_cells_coordinates.remove(open_cell_coordinates);
            }
        }
        // finite state
        if (empty_num==0) {
            if (black_num>white_num) {
                return Double.POSITIVE_INFINITY;
            }
            if (black_num<white_num) {
                return Double.NEGATIVE_INFINITY;
            }
            return Double.NaN;
        }
        return black_boarder_num + black_num - white_num;
    }

    /**
     * produce a list of optional states of the board game that can be reached from the current one in one turn
     */
    public List<State> get_sons() {
        List<State> sons = new ArrayList<State>();
//        Log.log("parent state game board:");
//        Log.log(this.toString());
        for (int[] empty_cell_coordinates:this.open_cells_coordinates) {
            int i=empty_cell_coordinates[0], j=empty_cell_coordinates[1];
            sons.add(new State(put_soldier(clone_game_board(), i, j, this.player),
                    Player.get_players().get(!this.player.black_player)));
        }
        return sons;
    }

    private static String[][] put_soldier(String[][] game_board, int i, int j, Player player) {
        game_board[i][j]=player.soldier;
        // find all lines "closed" by this soldier

//        Log.log("before putting " + player.soldier +" soldier at "+i+","+j);
//        Log.log(toString(game_board,null));
        int[]   horizontal_limits = {0, game_board.length},
                vertical_limits = {0, game_board[0].length};
        for (int horizontal_limit:horizontal_limits) {
            for (int m=j;
                 horizontal_limit<j?m>=horizontal_limit:m<horizontal_limit;
                 m+=horizontal_limit<j?-1:1) {
                if (!check_and_set_line(game_board,i,j,i,m,player)) {
                    break;
                }
            }
//            Log.log("after horizon limit "+horizontal_limit);
//            Log.log(toString(game_board,null));
        }
        for (int vertical_limit:vertical_limits) {
            for (int m=i;
                 vertical_limit<i?m>=vertical_limit:m<vertical_limit;
                 m+=vertical_limit<i?-1:1) {
                if (!check_and_set_line(game_board,i,j,m,j,player)) {
                    break;
                }
            }
//            Log.log("after vertical limit "+vertical_limit);
//            Log.log(toString(game_board,null));
        }
        for (int horizontal_limit:horizontal_limits) {
            for (int vertical_limit:vertical_limits) {
                for (int m=i, n=j;
                     (horizontal_limit<j?n>=horizontal_limit:n<horizontal_limit)
                             && (vertical_limit<i?m>=vertical_limit:m<vertical_limit);
                     n+=horizontal_limit<j?-1:1, m+=vertical_limit<i?-1:1) {
                    if (!check_and_set_line(game_board,i,j,m,n,player)) {
                        break;
                    }
                }
//                Log.log("after horizon & vertical limits "+horizontal_limit+", "+vertical_limit);
//                Log.log(toString(game_board,null));
            }
        }
        return game_board;
    }

    /**
     * complete a line of soldiers only in case of a contigous line of soldiers of any color
     * and that ends with a soldier with the same color on the other end
     * @param game_board
     * @param x
     * @param y
     * @param i
     * @param j
     * @param player
     * @return whther can continue searching for nearest soldier with the same players soldier color
     */
    private static boolean check_and_set_line(String[][] game_board, int i, int j, int k, int l, Player player) {
        // skip current positioned soldier
        if (i==k && j==l) {
            return true;
        }
        // complete a line of soldiers only in case of a contigous line of soldiers of any color
        // and that ends with a soldier with the same color on the other end
        if (game_board[k][l].equals("E")) {
            return false;
        }
        if (game_board[k][l].equals(player.soldier)) {
            // create a line from origin soldier to destination soldier
            if (k == i) {
                for (int n = j; l < j ? n > l : n < l; n += l < j ? -1 : 1) {
                    game_board[i][n] = player.soldier;
                }
            } else if (l == j) {
                for (int m = i; k < i ? m > k : m < k; m += k < i ? -1 : 1) {
                    game_board[m][j] = player.soldier;
                }
            } else {
                for (int m = i, n = j;
                     k < i ? m > k : m < k && l < j ? n > l : n < l;
                     m += k < i ? -1 : 1, n += l < j ? -1 : 1) {
                    game_board[m][n] = player.soldier;
                }
            }
            return false;
        }
        // soldier of the other kind was found
        return true;
    }

    protected String[][] clone_game_board() {
        String[][] cloned_game_board = new String[this.game_board.length][this.game_board[0].length];
        for (int i=0; i< this.game_board.length; i++) {
            for (int j = 0; j < this.game_board[i].length; j++) {
                cloned_game_board[i][j]=this.game_board[i][j];
            }
        }
        return cloned_game_board;
    }

    static String toString(String[][] game_board, Set<int[]> open_cells_coordinates) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i< game_board.length; i++) {
            for (int j=0; j< game_board[i].length; j++) {
                sb.append(open_cells_coordinates!=null&&open_cells_coordinates.contains(new int[] {i,j})
                        ?"O":game_board[i][j]);
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    static String toString(String[][] game_board, Set<int[]> open_cells_coordinates, Double value) {
        return "value="+value+"\n"+toString(game_board, open_cells_coordinates);
    }

    /**
     *
     * for debugging
     * @return
     */
    @Override
    public String toString() {
        return toString(this.game_board, this.open_cells_coordinates, this.value);
    }
}

public class java_ex2 {
    /**
     * @param state the state picked at the last turn
     * @param level the level of depth search
     * @param p the player currently playing. Either "W" or "B"
     * @return
     */
    static Choise play(State state, int level) {
        Log.log("level="+(3-level)+", player="+state.player.soldier+", state:\n"+state);
        // last level / win or lose state
        if (level==0 || Double.isInfinite(state.value)) {
            return new Choise(state, state.value);
        }
        Choise choise_this_layer = new Choise(null, Double. NaN);
        for (State son:state.get_sons()) {
            choise_this_layer = state.player.choose(choise_this_layer, play(son, level-1));
        }
        Log.log("chosen son value at level "+(3-level)+" by player "+state.player.soldier+" is: "+choise_this_layer.propagated_value + ", chosen state:\n"+choise_this_layer.state);
        return choise_this_layer;
    }

    static String search(String[][] game_board) {
        // start with the black player
        Choise choise = new Choise(new State(game_board, Player.get_players().get(true)), 0.0);
        // until victory or loss
        int level = 0;
        while (Double.isFinite(choise.propagated_value)) {
            Log.log("init state of player " + choise.state.player.soldier +" at level "+level+":\n"+choise.state);
            // look 3 levels onward
            choise = play(choise.state,3);
            Log.log("choise after 3 layers deep search for player "+choise.state.player.soldier+":\n"+choise.state);
        }
        return choise.propagated_value ==Double.POSITIVE_INFINITY?"B":"W";
    }

    protected static void produce_output(String output_file_path, String winning_player) throws IOException {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output_file_path)));
            writer.write(winning_player);
        } finally {
            try {writer.close();} catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

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
            // TODO change to local in & outfile
            produce_output("/home/nadav/workspaces/ml/ex2/resources/output_mine.txt", search(parse_input("/home/nadav/workspaces/ml/ex2/resources/input.txt")));
        } catch (Exception e) {
            e.printStackTrace();
            Log.close();
        }
    }
}