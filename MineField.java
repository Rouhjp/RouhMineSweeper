import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.*;

class MineField{
    private Status status = Status.READY;
    private List<Cell> cells = new ArrayList<>();
    private final int height;
    private final int width;
    private final int mines;
    private int openCount = 0;
    private MineField(int height, int width, int mines){
        this.height = height;
        this.width = width;
        this.mines = mines;
        IntStream.range(0, height*width).forEach(i->cells.add(new Cell(i)));
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
    private class Cell implements Cloneable{
        private static final int MINE = 9;
        private final int index;
        private int value = 0;
        private Face face = Face.DEFAULT;
        private Cell(int index){
            this.index = index;
        }
        private boolean isMine(){
            return this.value==MINE;
        }
        private List<Cell> aroundCells(){
            return IntStream.range(0, 9).filter(i->i!=4)
                    .mapToObj(i->new int[]{index%width+i%3-1,index/width+i/3-1})
                    .filter(p->p[0]>=0&&p[0]<width&&p[1]>=0&&p[1]<height)
                    .mapToInt(p->p[0] + p[1]*width)
                    .mapToObj(cells::get)
                    .collect(Collectors.toList());
        }
    }
    private enum Face{
        DEFAULT,FLAGGED,REMOVED
    }
    private void flagOpen(Cell target){
        if(target.value==target.aroundCells().stream().filter(cell->cell.face==Face.FLAGGED).count()){
            target.aroundCells().forEach(this::open);
        }
    }
    private void toggleFlag(Cell target){
        if(target.face==Face.DEFAULT){
            target.face = Face.FLAGGED;
        }else if(target.face==Face.FLAGGED){
            target.face = Face.DEFAULT;
        }
    }
    private void generate(Cell target){
        Random random = new Random(); // caution: this is LCG
        int count = 0;
        while(count<mines){
            int randomPoint = random.nextInt(height*width);
            if(Math.abs(randomPoint%width - target.index%width)>=2||
                    Math.abs(randomPoint/width - target.index/width)>=2){
                Cell randomTarget = cells.get(randomPoint);
                if(!randomTarget.isMine()){
                    randomTarget.value = Cell.MINE;
                    count++;
                }
            }
        }
        cells.stream().filter(cell->!cell.isMine())
                .forEach(cell->cell.value = (int)cell.aroundCells().stream().filter(Cell::isMine).count());
        this.status = Status.GENERATED;
    }
    private void open(Cell target){
        if(target.face==Face.DEFAULT){
            target.face = Face.REMOVED;
            if(++openCount==height*width-mines){
                this.status = Status.SECURED;
            }else{
                if(target.value==0){
                    target.aroundCells().forEach(this::open);
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
    private CellView getViewOf(Cell target){
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
        return getViewOf(cells.get(x + y*width));
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
            case READY: generate(cells.get(x + y*width));
            case GENERATED: open(cells.get(x + y*width));
        }
    }
    void toggleFlag(int x, int y){
        if(status!=Status.EXPLODED) toggleFlag(cells.get(x + y*width));
    }
    void flagOpen(int x, int y){
        if(status==Status.GENERATED) flagOpen(cells.get(x + y*width));
    }
    boolean isDefault(){
        return status==Status.READY;
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
        return mines - (int)cells.stream().filter(cell->cell.face==Face.FLAGGED).count();
    }
}
