import java.io.*;
import java.util.*;
import java.util.function.Function;

/**
 * a context class, encaptulating the response a search algorithm gives,
 * whether it has succeeded in finding a route and the route itself
 */
class Response {
    boolean has_reached_goal = false,
            went_over_all_states = false;
    List<Direction> way_to_goal = new ArrayList<Direction>();
    int way_cost;
}

/**
 * a stack holding the route found by the algorithm and returned to the user
 */
class AncestorsStack extends Stack<State> {
    /**
     * folds up to the ancestor which has children the searching algorithm can continue operating from
     */
    @Override
    public synchronized State pop() {
        State s = super.pop();
        if (this.empty()) {
            return s;
        }
        if (!this.isEmpty() && --this.peek().sons_candidate_for_route==0) {
            this.pop();
        }
        return s;
    }
}

/**
 * represent the direction in the resulting route from a parent state to its siebling
 * ordered from right to upper right, clockwise
 */
enum Direction implements Comparable<Direction> {
    R(7), RD(6), D(5), LD(4), L(3), LU(2), U(1), RU(0);

    /**
     * @return return the direction from the source state depending on the offset from its coordinates
     */
    public static Direction valueOf(int x_offset, int y_offset) {
        switch (x_offset) {
            case -1:
                switch (y_offset) {
                    case -1:
                        return Direction.LU;
                    case 0:
                        return Direction.U;
                    case 1:
                        return Direction.RU;
                }
            case 0:
                switch (y_offset)  {
                    case -1:
                        return Direction.L;
                    case 0:
                        return null;
                    case 1:
                        return Direction.R;
                }
            case 1:
                switch (y_offset) {
                    case -1:
                        return Direction.LD;
                    case 0:
                        return Direction.D;
                    case 1:
                        return Direction.RD;
                }
        }
        return null;
    }

    // priority - relevant for state son retrieval order
    int priority;

    Direction(int priority) {
        this.priority = priority;
    }
}

/**
 * represent an abstract of the state node used by the searching algorithms
 * holds within common state node data, such as its coordinates,
 * and a reference to the in memory matrix of input terrain,
 * from which its instantiate its children on the fly
 */
abstract class State {
    @Override
    public String toString() {
        return "<"+x+","+y+" t:"+this.input_matrix[x][y]+">";
    }

    protected String[][] input_matrix;
    protected int x, y;
    protected Direction direction_from_parent;
    public boolean is_goal = false;
    public int cost;
    // used for the ancestor folding process
    public int sons_candidate_for_route=0;

    public State(String[][] input_matrix, int x, int y, Direction direction_from_parent, boolean is_goal){
        this.input_matrix = input_matrix;
        this.x=x;
        this.y=y;
        this.direction_from_parent = direction_from_parent;
        this.is_goal=is_goal;
        switch (input_matrix[x][y]) {
            case "R":
                this.cost=1;
                break;
            case "D":
                this.cost=3;
                break;
            case "H":
                this.cost=10;
                break;
                // assuming water would be filtered by the algorithm before the point of weighting the distance to the water state is reached
            default:
                this.cost=0;
        }
    }

    /**
     * create a son to the given sons list, only in case it is a legal son per the input terrain
     * (not diagonal to its parent if water is in between, not a water node itself, within the board boundries, etc.)
     */
    protected State create_son(int x, int y, Direction direction_fron_parent) {
        // out of bounds
        if (x<0 || y<0 || x>=this.input_matrix.length || y>=this.input_matrix.length
                // water / near water
                || this.input_matrix[x][y].equals("W")) {
            return null;
        }
        // check there are no water in direct line next to the origin when walking diagonally
        for (int i=-1; i<=1; i+=2) {
            for (int j=-1; j<=1; j+=2) {
                if (this.x+i==x && this.y+j==y
                        && (this.input_matrix[this.x][y].equals("W") || this.input_matrix[x][this.y].equals("W"))) {
                    return null;
                }
            }
        }
        return create_son(this.input_matrix, x, y, direction_fron_parent, x==this.input_matrix.length-1 && y==this.input_matrix.length-1);
    }

    /**
     * used as part of the duplicate pruning process
     */
    @Override
    public boolean equals(Object obj) {
        State other = (State) obj;
        return this.x==other.x && this.y==other.y;
    }

    /**
     * create a son of the type of IDSState/ AStarState
     */
    protected abstract State create_son(String[][] input_matrix, int x, int y, Direction direction_from_parent, boolean is_goal);

    /**
     * append a son to the given sons list, only in case it is a legal son per the input terrain
     */
    protected abstract void add_son(List<State> sons, int x, int y, Direction direction_fron_parent);

    /**
     * @return an ordered list of sons from right to the parent state node to the upper-right, clockwise
     */
    public List<State> get_sons() {
        List<State> sons = new ArrayList<State>();
        for (int i=-1; i<=1; i++) {
            for (int j=-1; j<=1; j++) {
                if (!(i==0 && j==0)) {
                    this.add_son(sons,this.x+i, this.y+j, Direction.valueOf(i,j));
                }
            }
        }
        Collections.sort(sons, (s1, s2) -> s1.direction_from_parent.priority - s2.direction_from_parent.priority);
        return sons;
    }
}

/**
 * represent an IDS state, holding IDS specific state charectaristics,
 * such as node level, etc.
 */
class IDSState extends State {
    public int level;

    public IDSState(String[][] input_matrix, int x, int y, Direction direction_from_parent, boolean is_goal, int level) {
        super(input_matrix, x, y, direction_from_parent, is_goal);
        this.level=level;
    }

    /**
     * create a son of the type of IDSState
     */
    protected State create_son(String[][] input_matrix, int x, int y, Direction direction_from_parent, boolean is_goal) {
        return new IDSState(input_matrix, x, y, direction_from_parent, is_goal, this.level+1);
    }

    /**
     * append a son to the given sons list, only in case it is a legal son per the input terrain
     */
    protected void add_son(List<State> sons, int x, int y, Direction direction_fron_parent) {
        State son = this.create_son(x, y, direction_fron_parent);
        if (son != null) {
            sons.add((IDSState) son);
        }
    }
}

/**
 * represent an A* state, holding A* specific state charectaristics,
 * such as heuristic of distance from the goal, the cost of getting to this state from the starting point,
 * etc.
 */
class AStarState extends State {
    @Override
    public String toString() {
        return "<"+x+","+y+" t:"+this.input_matrix[x][y]+" g:"+this.g+" h:"+ this.h+">";
    }
    // the aerial distance from the goal
    protected int h;
    //  the cost of getting to this state
    protected int g;

    public AStarState(String[][] input_matrix, int x, int y, Direction direction_from_parent, boolean is_goal,
                      int parent_route_cost) {
        super(input_matrix, x, y, direction_from_parent, is_goal);
        // using aerial distance for heuristic distance - that is - the distance assuming the path goes through no water,
        // mountains, or any other obstacle.
        this.h = Math.max(input_matrix.length-x, input_matrix.length-y);
        this.g=parent_route_cost+this.cost;
    }

    /**
     * @return the sum of the heuristic distance from the goal and the cost of getting to this state
     */
    public int f() {
        return this.g + this.h;
    }

    /**
     * create a son of the type of AStarState
     */
    protected State create_son(String[][] input_matrix, int x, int y, Direction direction_from_parent, boolean is_goal) {
        if (x==0 && y==0) { return null;}
        return new AStarState(input_matrix, x, y, direction_from_parent, is_goal, this.g);
    }

    /**
     * append a son to the given sons list, only in case it is a legal son per the input terrain
     */
    @Override
    protected void add_son(List<State> sons, int x, int y, Direction direction_fron_parent) {
        State son = this.create_son(x, y, direction_fron_parent);
        if (son != null) {
            sons.add((AStarState) son);
        }
    }

    /**
     * @return a sorted list of sieblings in order of minimal heuristic + actual cost to each
     */
    @Override
    public List<State> get_sons() {
        List<State> sons = super.get_sons();
        Collections.sort(sons,
                (s1,s2)-> ((AStarState) s1).f()!=((AStarState) s2).f()
                        ?((AStarState) s2).f()-((AStarState) s1).f()
                        :s1.direction_from_parent.priority - s2.direction_from_parent.priority);
        return sons;
    }
}

/**
 * an abstract wrapping executor of the IDS and A* algorithms,
 * implementing a major part of their common functionality,
 * mainly the in depth search used by both
 */
abstract class SearchAlgorithm {
    static void extract_way_and_cost(Response response, Stack<State> ancestors) {
        List<Direction> directions = new ArrayList<Direction>();
        int way_cost = 0;
        for (State s:ancestors) {
            // the starting point
            if (s.direction_from_parent == null) { continue; }
            directions.add(s.direction_from_parent);
            way_cost+=s.cost;
        }
        if (response.way_cost==0 || response.way_cost>way_cost) {
            response.way_to_goal = directions;
            response.way_cost = way_cost;
        }
    }

    public abstract Response run(String[][] input_matrix);

    /**
     * used as part of this algorithm in order to make sure no duplicated states are
     * added to the route this algorithm finds
     * @param states_to_visit the stack which holds the states to be visited
     * @param ancestors the stack which holds the actual route to be returned to the user
     * @param s the state to examine
     * @return
     */
    protected abstract boolean is_dup_pruning(Stack<State> states_to_visit, AncestorsStack ancestors, State s);

    /**
     * execute an in depth search for the goal
     * @param starting_state the root of this search tree
     * @param is_limited a limitation the algorithm can apply on the sons it searches through,
     *                   such as their level (by the IDS algorithm)
     * @param is_iteration_limit_met a limitation over the amount of iterations the A* algorithm is allowed to run.
     *                               used in to put a stopping condition to situations where the goal is unreachcable
     *                               for IDS this condition is irrelevant
     */
    protected Response depth_search(State starting_state, Function<State, Boolean> is_limited,
                                    Function<Integer, Boolean> is_iteration_limit_met,
                                    Function<Response, Boolean> should_stop_on_goal_met) {
        Response response = new Response();
        Stack<State> states_to_visit = new Stack<State>();
        AncestorsStack ancestors = new AncestorsStack();
        states_to_visit.push(starting_state);
        for (int i=0;
             !(should_stop_on_goal_met.apply(response) || states_to_visit.isEmpty() || is_iteration_limit_met.apply(i));
             i++) {
            State s = states_to_visit.pop();
            ancestors.push(s);
            if (s.is_goal) {
                SearchAlgorithm.extract_way_and_cost(response, ancestors);
                ancestors.pop();
                response.has_reached_goal = true;
                continue;
            }
            List<State> sons = s.get_sons();
            if (sons.isEmpty()) {
                ancestors.pop();
                continue;
            }
            int contributed_sons = 0;
            for (State son:sons) {
                // enforce duplicate pruning
                //TODO: does dup pruning means that no repetitions over ancestors or also over candidates?
                if (this.is_dup_pruning(states_to_visit, ancestors, son)) {
                // enforce additional limitation the algorithm apply
                } else if (is_limited.apply(son)) {
                } else {
                    states_to_visit.push(son);
                    contributed_sons++;
                }
            }
            // start folding process
            if (contributed_sons==0) {
                ancestors.pop();
                continue;
            }
            s.sons_candidate_for_route=contributed_sons;
        }
        return response;
    }
}

/**
 * the wrappint executor of the IDS algorithm
 * instantiating the search nodes (states) on the fly out of the in memory matrix
 * each choise is made based on its geographical direction from its parent state
 * and its reachability (no diagonal walks if water is in between, cant walk over water)
 */
class IDS extends SearchAlgorithm {
    /**
     * used as part of this algorithm in order to make sure no duplicated states are
     * added to the route this algorithm finds
     * @param states_to_visit the stack which holds the states to be visited
     * @param ancestors the stack which holds the actual route to be returned to the user
     * @param s the state to examine
     * @return
     */
    protected boolean is_dup_pruning(Stack<State> states_to_visit, AncestorsStack ancestors, State s) {
        return ancestors.contains(s) || states_to_visit.contains(s);
    }

    /**
     * run in depth search in an iterative manner, until the goal is met,
     * or the limit over the number of levels is reached
     */
    public Response run(String[][] input_matrix) {
        Response response = new Response();
        IDSState starting_state = new IDSState(input_matrix, 0, 0, null, false, 0);
        for (int level_limit=0;
             !(response.has_reached_goal || response.went_over_all_states
                     // limiting the search to iterate at most to a level equals to
                     // the number of elements in the matrix
                     || level_limit>=input_matrix.length*input_matrix.length) ;
             level_limit++) {
            final int level_limit_ = level_limit;
            response = this.depth_search(starting_state, s -> ((IDSState) s).level>=level_limit_, i -> false, r -> r.has_reached_goal);
        }
        return response;
    }
}

/**
 * the wrappint executor of the AStar algorithm
 * instantiating the search nodes (states) on the fly of the in memory matrix
 * each choise is made based on the heuristic function of the chosen node, compound of
 * the difficuly to reach it from the starting point along with a heuristic of its distance
 * from the goal, and based on its geographical direction from its parent state
 * and its reachability (no diagonal walks if water is in between, cant walk over water)
 */
class AStar extends SearchAlgorithm {
    public Response run(String[][] input_matrix) {
        return this.depth_search(
                new AStarState(input_matrix, 0, 0, null, false, 0),
                s -> false, i -> i>input_matrix.length*input_matrix.length*100, r -> false);
    }

    /**
     * used as part of this algorithm in order to make sure no duplicated states are
     * added to the route this algorithm finds
     * @param states_to_visit the stack which holds the states to be visited
     * @param ancestors the stack which holds the actual route to be returned to the user
     * @param s the state to examine
     * @return
     */
    @Override
    protected boolean is_dup_pruning(Stack<State> states_to_visit, AncestorsStack ancestors, State s) {
        return ancestors.contains(s);
    }
}

public class java_ex1 {
    protected static void produce_output(String output_file_path, Response response) throws IOException {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output_file_path)));
            if (response.has_reached_goal) {
                StringBuilder sb = new StringBuilder();
                for (Direction d : response.way_to_goal) {
                    sb.append(d+"-");
                }
                sb.deleteCharAt(sb.length()-1);
                sb.append(" "+response.way_cost);
                writer.write(sb.toString());
            } else {
                writer.write("no path");
            }
        } finally {
            try {writer.close();} catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected static Object[] parse_input(String input_file_path) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(input_file_path));
            Object[] parsed_input = new Object[2];
            parsed_input[0] = br.readLine().equals("IDS")?new IDS():new AStar();
            int matrix_len = Integer.valueOf(br.readLine());
            String[][] input_matrix = new String[matrix_len][matrix_len];
            parsed_input[1]=input_matrix;
            for (int i=0; i< matrix_len; i++) {
                input_matrix[i]=br.readLine().split("");
            }
            return parsed_input;
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        Object[] parsed_input = new Object[2];
        try {
            parsed_input = parse_input("input.txt");
            SearchAlgorithm search_algorithm = (SearchAlgorithm) parsed_input[0];
            String[][] input_matrix = (String[][]) parsed_input[1];
            // run the algorithm over the matrix
            Response response = search_algorithm.run(input_matrix);
            // print output
            produce_output("output.txt", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
