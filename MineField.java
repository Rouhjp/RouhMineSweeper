import java.util.Arrays;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.*;

class MineField{
    private Status status = Status.READY;
    private Cell[][] cells;
    private final int height;
    private final int width;
    private final int mines;
    private int openCount = 0;
    private MineField(int height, int width, int mines){
        this.height = height;
        this.width = width;
        this.mines = mines;
        this.cells = new Cell[height][width];
        for(int i = 0;i<height;i++){
            for(int j = 0;j<width;j++){
                this.cells[i][j] = new Cell();
            }
        }
    }
    MineField(Difficulty d){
        this(d.height, d.width, d.mines);
    }
    public enum Difficulty{
        BEGINNER(9, 9, 10),
        INTERMEDIATE(16, 16, 40),
        ADVANCED(16, 30, 99);
        private final int height;
        private final int width;
        private final int mines;
        Difficulty(int height, int width, int mines){
            this.height = height;
            this.width = width;
            this.mines = mines;
        }
    }
    private enum Status{
        READY,GENERATED,EXPLODED,SECURED
    }
    private class Cell{
        private static final int MINE = 9;
        private Face face = Face.DEFAULT;
        private int value = 0;
        private boolean isMine(){
            return this.value==MINE;
        }
        private void removeFace(){
            this.face = Face.REMOVED;
        }
    }
    private enum Face{
        DEFAULT,FLAGGED,REMOVED
    }
    private class Point{
        private int x;
        private int y;
        private Point(int x, int y){
            this.x = x;
            this.y = y;
        }
        private boolean isWithin(){
            return x>=0&&y>=0&&x<width&&y<height;
        }
        private Point[] getAroundPoints(){
            return IntStream.range(0, 9).filter(i->i!=4).mapToObj(i->new Point(x+i%3-1, y+i/3-1))
                    .filter(Point::isWithin).toArray(Point[]::new);
        }
    }
    private Cell getCellOf(Point point){
        return cells[point.y][point.x];
    }
    private int countAroundCellOf(Point point, Predicate<Cell> check){
        return (int)Arrays.stream(point.getAroundPoints()).map(this::getCellOf).filter(check).count();
    }
    private void flagOpen(Point point){
        if(getCellOf(point).value==countAroundCellOf(point, cell->cell.face==Face.FLAGGED)){
            Arrays.stream(point.getAroundPoints()).forEach(this::open);
        }
    }
    private void toggleFlag(Point point){
        Cell target =getCellOf(point);
        if(target.face==Face.DEFAULT){
            target.face = Face.FLAGGED;
        }else if(target.face==Face.FLAGGED){
            target.face = Face.DEFAULT;
        }
    }
    private void generate(Point point){
        Random random = new Random(); // caution: this is LCG
        int count = 0;
        while(count<mines){
            Point randomPoint = new Point(random.nextInt(width), random.nextInt(height));
            if(Math.abs(randomPoint.x - point.x)>=2||Math.abs(randomPoint.y - point.y)>=2){
                Cell target = getCellOf(randomPoint);
                if(!target.isMine()){
                    target.value = Cell.MINE;
                    count++;
                }
            }
        }
        for(int i = 0;i<height;i++){
            for(int j = 0;j<width;j++){
                if(!cells[i][j].isMine()){
                    cells[i][j].value = countAroundCellOf(new Point(j, i), Cell::isMine);
                }
            }
        }
        this.status = Status.GENERATED;
    }
    private void open(Point point){
        Cell target = getCellOf(point);
        if(target.face==Face.DEFAULT){
            target.removeFace();
            if(++openCount==height*width-mines){
                this.status = Status.SECURED;
            }else{
                if(target.value==0){
                    Arrays.stream(point.getAroundPoints()).forEach(this::open);
                }else if(target.isMine()){
                    this.status = Status.EXPLODED;
                }
            }
        }
    }
    @SuppressWarnings("unused")
    public enum CellView{
        M0,M1,M2,M3,M4,M5,M6,M7,M8,MINE,COVERED, FLAGGED, STILL_COVERED, MISS_FLAGGED
    }
    private CellView getViewOf(Point point){
        Cell target = getCellOf(point);
        if(isFinished()){
            switch(target.face){
                case DEFAULT: return target.isMine()? CellView.STILL_COVERED: CellView.COVERED;
                case FLAGGED: return target.isMine()? CellView.FLAGGED: CellView.MISS_FLAGGED;
                case REMOVED: return CellView.values()[target.value];
            }
        }else{
            switch(target.face){
                case DEFAULT: return CellView.COVERED;
                case FLAGGED: return CellView.FLAGGED;
                case REMOVED: return CellView.values()[target.value];
            }
        }
        throw new IllegalArgumentException();
    }
    CellView getViewOf(int x, int y){
        return getViewOf(new Point(x, y));
    }
    CellView[][] getView(){
        CellView[][] view = new CellView[height][width];
        for(int i = 0;i<height;i++){
            for(int j = 0;j<width;j++){
                view[i][j] = getViewOf(j, i);
            }
        }
        return view;
    }
    @SuppressWarnings("fallthrough")
    void open(int x, int y){
        switch(status){
            case READY: generate(new Point(x, y));
            case GENERATED: open(new Point(x, y));
        }
    }
    void toggleFlag(int x, int y){
        if(status!=Status.EXPLODED) toggleFlag(new Point(x, y));
    }
    void flagOpen(int x, int y){
        if(status==Status.GENERATED) flagOpen(new Point(x, y));
    }
    boolean isFinished(){
        return status==Status.EXPLODED||status==Status.SECURED;
    }
    boolean isSecured(){
        return status==Status.SECURED;
    }
    boolean isRunning(){
        return status==Status.GENERATED;
    }
    int getHeight(){
        return height;
    }
    int getWidth(){
        return width;
    }
    int getMines(){
        return mines;
    }
    int getMineCount(){
        return mines - (int)Arrays.stream(cells).flatMap(Arrays::stream).filter(cell->cell.face==Face.FLAGGED).count();
    }
}
