package review2;

import java.util.*;

public class SignPostGame {

    static int size = 3;
    static int[][] board = new int[size][size];

    static char[][] dir = {
            {'R', 'R', 'D'},
            {'D', 'L', 'L'},
            {'R', 'R', 'E'}
    };

    static List<Integer>[] graph;

    static int humanScore = 10;
    static int computerScore = 10;

    static int currentPlayer = 1; // 1 = Human, 2 = Computer
    static boolean gameOver = false;

    // ================= MAIN =================
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        int r = 0, c = 0;
        board[0][0] = 1; // Auto place 1
        int currentNumber = 1;

        initializeGraph();

        while (currentNumber < 8 && !gameOver) {

            printBoard();
            System.out.println("\nHuman: " + humanScore + " | Computer: " + computerScore);

            if (currentPlayer == 1) {
                System.out.println("\nHUMAN TURN");
                System.out.print("Enter row col: ");
                int nr = sc.nextInt();
                int nc = sc.nextInt();

                if (isValidMove(r, c, nr, nc)) {
                    currentNumber++;
                    board[nr][nc] = currentNumber;
                    r = nr;
                    c = nc;
                    humanScore += 10;
                } else {
                    humanScore -= 5;
                    System.out.println("Wrong move! -5");
                    if (humanScore <= 0) {
                        humanScore = 0;
                        gameOver = true;
                        break;
                    }
                    continue;
                }

                currentPlayer = 2;
            } else {

                System.out.println("\nCOMPUTER TURN");

                int[] move = findComputerMove(r, c);

                if (move[0] == -1) {
                    computerScore -= 5;
                } else {
                    currentNumber++;
                    board[move[0]][move[1]] = currentNumber;
                    r = move[0];
                    c = move[1];
                    computerScore += 10;
                    System.out.println("Computer placed at: " + r + " " + c);
                }

                currentPlayer = 1;
            }
        }

        if (!gameOver) {
            board[2][2] = 9;
            System.out.println("\n9 Auto Placed at (2,2)");
        }

        printFinalResult();
        sc.close();
    }

    // ================= GRAPH =================
    static void initializeGraph() {
        graph = new ArrayList[size * size];

        for (int i = 0; i < graph.length; i++)
            graph[i] = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                int from = i * size + j;

                switch (dir[i][j]) {
                    case 'R':
                        if (j + 1 < size) graph[from].add(i * size + j + 1);
                        break;
                    case 'L':
                        if (j - 1 >= 0) graph[from].add(i * size + j - 1);
                        break;
                    case 'U':
                        if (i - 1 >= 0) graph[from].add((i - 1) * size + j);
                        break;
                    case 'D':
                        if (i + 1 < size) graph[from].add((i + 1) * size + j);
                        break;
                }
            }
        }
    }

    // ================= AI =================
    static int[] findComputerMove(int r, int c) {

        List<int[]> moves = getPossibleMoves(r, c);
        if (moves.isEmpty()) return new int[]{-1, -1};

        List<MoveScore> scored = new ArrayList<>();

        for (int[] move : moves) {

            int score = evaluateMove(move[0], move[1]);

            if (score > -1000)
                scored.add(new MoveScore(move, score));
        }

        if (scored.isEmpty()) return new int[]{-1, -1};

        // Mandatory Divide & Conquer
        mergeSortMoves(scored);

        return scored.get(0).move;
    }

    // ================= HEURISTIC =================
    static int evaluateMove(int r, int c) {

        int start = r * size + c;
        int end = 2 * size + 2;

        int dist = dijkstraDistance(start, end);

        if (dist == Integer.MAX_VALUE)
            return -1000;

        int score = 50 - dist * 5;

        score += countEmptyNeighbors(r, c) * 5;

        return score;
    }

    // ================= DIJKSTRA (Greedy) =================
    static int dijkstraDistance(int start, int end) {

        int[] dist = new int[size * size];
        Arrays.fill(dist, Integer.MAX_VALUE);

        PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> a.dist - b.dist);

        dist[start] = 0;
        pq.add(new Node(start, 0));

        while (!pq.isEmpty()) {

            Node curr = pq.poll();

            if (curr.vertex == end)
                return dist[end];

            for (int nei : graph[curr.vertex]) {

                int nr = nei / size;
                int nc = nei % size;

                int cost = (board[nr][nc] == 0 || nei == end) ? 1 : 100;

                if (dist[curr.vertex] + cost < dist[nei]) {
                    dist[nei] = dist[curr.vertex] + cost;
                    pq.add(new Node(nei, dist[nei]));
                }
            }
        }

        return Integer.MAX_VALUE;
    }

    // ================= MERGE SORT =================
    static void mergeSortMoves(List<MoveScore> moves) {

        if (moves.size() <= 1) return;

        int mid = moves.size() / 2;

        List<MoveScore> left = new ArrayList<>(moves.subList(0, mid));
        List<MoveScore> right = new ArrayList<>(moves.subList(mid, moves.size()));

        mergeSortMoves(left);
        mergeSortMoves(right);

        merge(moves, left, right);
    }

    static void merge(List<MoveScore> result,
                      List<MoveScore> left,
                      List<MoveScore> right) {

        int i = 0, j = 0, k = 0;

        while (i < left.size() && j < right.size()) {

            if (left.get(i).score >= right.get(j).score)
                result.set(k++, left.get(i++));
            else
                result.set(k++, right.get(j++));
        }

        while (i < left.size())
            result.set(k++, left.get(i++));

        while (j < right.size())
            result.set(k++, right.get(j++));
    }

    // ================= HELPERS =================
    static int countEmptyNeighbors(int r, int c) {

        int count = 0;
        int[][] d = {{-1,0},{1,0},{0,-1},{0,1}};

        for (int[] x : d) {
            int nr = r + x[0];
            int nc = c + x[1];

            if (nr>=0 && nr<size && nc>=0 && nc<size && board[nr][nc]==0)
                count++;
        }

        return count;
    }

    static List<int[]> getPossibleMoves(int r, int c) {

        List<int[]> list = new ArrayList<>();
        char d = dir[r][c];

        if (d=='R' && c+1<size && board[r][c+1]==0)
            list.add(new int[]{r,c+1});

        if (d=='L' && c-1>=0 && board[r][c-1]==0)
            list.add(new int[]{r,c-1});

        if (d=='U' && r-1>=0 && board[r-1][c]==0)
            list.add(new int[]{r-1,c});

        if (d=='D' && r+1<size && board[r+1][c]==0)
            list.add(new int[]{r+1,c});

        return list;
    }

    static boolean isValidMove(int r,int c,int nr,int nc){

        if(nr<0||nr>=size||nc<0||nc>=size) return false;
        if(board[nr][nc]!=0) return false;

        char d=dir[r][c];

        if(d=='R') return nr==r && nc==c+1;
        if(d=='L') return nr==r && nc==c-1;
        if(d=='U') return nr==r-1 && nc==c;
        if(d=='D') return nr==r+1 && nc==c;

        return false;
    }

    static void printBoard(){

        System.out.println("\nBOARD:");
        for(int i=0;i<size;i++){
            for(int j=0;j<size;j++){
                if(board[i][j]==0){
                    if(i==2&&j==2) System.out.print("[9] ");
                    else System.out.print("[ ] ");
                } else {
                    System.out.print("["+board[i][j]+"] ");
                }
            }
            System.out.println();
        }
    }

    static void printFinalResult(){

        System.out.println("\nFINAL SCORE:");
        System.out.println("Human: "+humanScore);
        System.out.println("Computer: "+computerScore);

        if(gameOver)
            System.out.println("GAME OVER - Human reached 0");
        else if(humanScore>computerScore)
            System.out.println("HUMAN WINS");
        else if(computerScore>humanScore)
            System.out.println("COMPUTER WINS");
        else
            System.out.println("DRAW");
    }

    static class Node{
        int vertex,dist;
        Node(int v,int d){vertex=v;dist=d;}
    }

    static class MoveScore{
        int[] move;
        int score;
        MoveScore(int[] m,int s){move=m;score=s;}
    }
}
