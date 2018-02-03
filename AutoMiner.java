
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class AutoMiner{
    private MineField field;
    private List<Cell> cells = new ArrayList<>();
    private final int height;
    private final int width;
    AutoMiner(MineField field){
        this.height = field.getHeight();
        this.width = field.getWidth();
        this.field = field;
        this.cells = getCells(field);
    }
    void execute(){
        if(field.isDefault()){
            field.open(field.getWidth()/2, field.getHeight()/2);
        }else if(field.isRunning()){
            clearFlag(cells);
            for(int i=0;i<2;i++){
                fullCountFlagger(cells);
                fullCountMiner(cells);
                if(!accept()){
                    absurdDeductionMiner();
                    if(!accept()) break;
                }
            }
        }
    }
    private List<Cell> getCells(MineField field){
        return IntStream.range(0, width*height)
                .mapToObj(i->new Cell(i, getValueOf(field.getViewOf(i%width,i/width))))
                .collect(Collectors.toList());
    }
    private int getValueOf(MineField.CellView view){
        switch(view){
            case FLAGGED: return Cell.FLAGGED;
            case COVERED: return Cell.COVERED;
            default: return view.ordinal();
        }
    }
    private class Cell{
        private static final int COVERED = 10;
        private static final int FLAGGED = 11;
        private static final int REMOVED = 12;
        private int index = 0;
        private int value = 0;
        private Cell(int index, int value){
            this.index = index;
            this.value = value;
        }
        private List<Cell> aroundCells(List<Cell> cells){
            return IntStream.range(0, 9).filter(i->i!=4)
                    .mapToObj(i->new int[]{index%width+i%3-1,index/width+i/3-1})
                    .filter(p->p[0]>=0&&p[0]<width&&p[1]>=0&&p[1]<height)
                    .mapToInt(p->p[0] + p[1]*width)
                    .mapToObj(cells::get)
                    .collect(Collectors.toList());
        }
        private boolean isCovered(){
            return value==COVERED;
        }
        private boolean isFlagged(){
            return value==FLAGGED;
        }
        private boolean isRemoved(){
            return value==REMOVED;
        }
        private void removeFlag(){
            this.value = COVERED;
        }
        private void flag(){
            this.value = FLAGGED;
        }
        private void open(){
            this.value = REMOVED;
        }
        private void update(){
            this.value = getValueOf(field.getViewOf(index%width, index/width));
        }
    }
    private void clearFlag(List<Cell> cells){
        cells.forEach(Cell::removeFlag);
    }
    private static void fullCountFlagger(List<Cell> cells){
        cells.stream().filter(cell->cell.value>0&&cell.value<9)
                .forEach(cell->{
                    int flaggedCount = (int)cell.aroundCells(cells).stream().filter(Cell::isFlagged).count();
                    int coveredCount = (int)cell.aroundCells(cells).stream().filter(Cell::isCovered).count();
                    if(cell.value - flaggedCount==coveredCount){
                        cell.aroundCells(cells).stream().filter(Cell::isCovered).forEach(Cell::flag);
                    }
                });
    }
    private static void fullCountMiner(List<Cell> cells){
        cells.stream().filter(cell->cell.value>0&&cell.value<9)
                .forEach(cell->{
                    int flaggedCount = (int)cell.aroundCells(cells).stream().filter(Cell::isFlagged).count();
                    if(cell.value==flaggedCount){
                        cell.aroundCells(cells).stream().filter(Cell::isCovered).forEach(Cell::open);
                    }
                });
    }
    private static boolean isConsistent(List<Cell> virtual){
        return virtual.stream().filter(cell->cell.value<9)
                .noneMatch(cell->{
                    int flaggedCount = (int)cell.aroundCells(virtual).stream().filter(Cell::isFlagged).count();
                    int coveredCount = (int)cell.aroundCells(virtual).stream().filter(Cell::isCovered).count();
                    return cell.value<flaggedCount||cell.value - flaggedCount>coveredCount;
                });
    }
    private void absurdDeductionMiner(){
        IntStream.range(0, height*width).mapToObj(cells::get).filter(cell->{
            int flaggedCount = (int)cell.aroundCells(cells).stream().filter(Cell::isFlagged).count();
            int coveredCount = (int)cell.aroundCells(cells).stream().filter(Cell::isCovered).count();
            return coveredCount>cell.value - flaggedCount;
        }).forEach(cell->{
            int[] flaggedMap = new int[8];
            int[] removedMap = new int[8];
            int count = (int)tentativeFlagger(cells, cell.index, 0).stream()
                    .peek(AutoMiner::fullCountMiner)
                    .filter(AutoMiner::isConsistent)
                    .peek(virtual->{
                        List<Cell> around = cell.aroundCells(virtual);
                        IntStream.range(0, around.size()).forEach(i->{
                            Cell target = around.get(i);
                            if(target.isRemoved()) removedMap[i]++;
                            if(target.isFlagged()) flaggedMap[i]++;
                        });
                    }).count();
            if(count>0){
                IntStream.range(0, 8).forEach(i->{
                    if(flaggedMap[i]==count) cell.aroundCells(cells).get(i).flag();
                    if(removedMap[i]==count) cell.aroundCells(cells).get(i).open();
                });
            }
        });
    }
    private List<Cell> deepCopyOf(List<Cell> original){
        return original.stream().map(cell->new Cell(cell.index, cell.value)).collect(Collectors.toList());
    }
    private List<List<Cell>> tentativeFlagger(List<Cell> virtual, int baseIndex, int startIndex){
        return virtual.get(baseIndex).aroundCells(virtual).stream()
                .filter(target->target.index>=startIndex)
                .filter(Cell::isCovered)
                .map(target->{
                    List<Cell> child = deepCopyOf(virtual);
                    child.get(target.index).flag();
                    int flaggedCount = (int)virtual.get(baseIndex).aroundCells(child).stream().filter(Cell::isFlagged).count();
                    return virtual.get(baseIndex).value>flaggedCount?
                            tentativeFlagger(child, baseIndex, target.index):Collections.singletonList(child);
                }).flatMap(List::stream).collect(Collectors.toList());
    }
    private boolean accept(){
        return IntStream.range(0, height*width)
                .filter(i->getValueOf(field.getViewOf(i%width, i/width))!=cells.get(i).value)
                .peek(i->{
                    if(cells.get(i).value==Cell.REMOVED) field.open(i%width, i/width);
                    if(cells.get(i).value==Cell.FLAGGED) field.toggleFlag(i%width, i/width);
                    cells.get(i).update();
                }).count()>0;
    }
    /*
    private void show(List<Cell> cells){
        System.out.println();
        cells.forEach(cell->{
            System.out.print(cell.index%width==0? "|":"");
            System.out.print(new String[]{"　","１","２","３","４","５","６","７","８","※","■","◆","□"}[cell.value]+"|");
            System.out.print(cell.index%width==width-1? "\n":"");
        });
    }
    //*/
}
