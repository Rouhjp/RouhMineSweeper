import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class AutoMiner{
    private MineField game;
    private final int height;
    private final int width;
    AutoMiner(MineField field){
        this.height = field.getHeight();
        this.width = field.getWidth();
        this.game = field;
    }
    void execute(){
        if(game.isDefault()){
            game.open(width/2, height/2);
        }else if(game.isRunning()){
            List<Cell> cells = getCells(game);
            clearing(cells);
            simpleMining(cells);
            absurdDeductionMining(cells);
            accept(cells);
        }
    }
    private List<Cell> getCells(MineField field){
        return IntStream.range(0, width*height)
                .mapToObj(i->new Cell(i, getValueOf(field.getViewOf(i%width, i/width))))
                .collect(Collectors.toList());
    }
    private static int getValueOf(MineField.CellView view){
        switch(view){
            case FLAGGED: return Cell.FLAGGED;
            case COVERED: return Cell.COVERED;
            default: return view.ordinal();
        }
    }
    private class Cell implements Cloneable{
        private static final int COVERED = 10;
        private static final int FLAGGED = 11;
        private static final int REMOVED = 12;
        private int index = 0;
        private int value = 0;
        private Cell(int index, int value){
            this.index = index;
            this.value = value;
        }
        @Override
        public Cell clone(){
            Cell cell = null;
            try{
                cell = (Cell)super.clone();
            }catch(CloneNotSupportedException e){
                e.printStackTrace();
            }
            return cell;
        }
        private List<Cell> aroundCells(List<Cell> cells){
            return IntStream.range(0, 9).filter(i->i!=4)
                    .mapToObj(i->new int[]{index%width + i%3 - 1, index/width + i/3 - 1})
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
            this.value = getValueOf(game.getViewOf(index%width, index/width));
        }
    }
    private void accept(List<Cell> cells){
        cells.stream().filter(Cell::isFlagged)
                .filter(cell->game.getViewOf(cell.index%width, cell.index/width)==MineField.CellView.COVERED)
                .peek(cell->game.toggleFlag(cell.index%width, cell.index/width))
                .forEach(Cell::update);
        cells.stream().filter(Cell::isRemoved)
                .peek(cell->game.open(cell.index%width, cell.index/width))
                .forEach(Cell::update);
    }
    private static void clearing(List<Cell> cells){
        cells.stream().filter(Cell::isFlagged).forEach(Cell::removeFlag);
    }
    private static void simpleMining(List<Cell> cells){
        for(;;) if(fullCountFlagging(cells)&fullCountMining(cells)) break;
    }
    private static boolean fullCountFlagging(List<Cell> cells){
        return cells.stream().filter(cell->cell.value>0&&cell.value<9)
                .filter(cell->{
                    int flaggedCount = (int)cell.aroundCells(cells).stream().filter(Cell::isFlagged).count();
                    int coveredCount = (int)cell.aroundCells(cells).stream().filter(Cell::isCovered).count();
                    return coveredCount>0&&cell.value - flaggedCount==coveredCount;
                }).peek(cell->cell.aroundCells(cells).stream().filter(Cell::isCovered).forEach(Cell::flag)).count()==0;
    }
    private static boolean fullCountMining(List<Cell> cells){
        return cells.stream().filter(cell->cell.value>0&&cell.value<9)
                .filter(cell->{
                    int flaggedCount = (int)cell.aroundCells(cells).stream().filter(Cell::isFlagged).count();
                    int coveredCount = (int)cell.aroundCells(cells).stream().filter(Cell::isCovered).count();
                    return coveredCount>0&&cell.value==flaggedCount;
                }).peek(cell->cell.aroundCells(cells).stream().filter(Cell::isCovered).forEach(Cell::open)).count()==0;
    }
    private static boolean isConsistent(List<Cell> virtual){
        return virtual.stream().filter(cell->cell.value>0&&cell.value<9).noneMatch(cell->{
            int flaggedCount = (int)cell.aroundCells(virtual).stream().filter(Cell::isFlagged).count();
            int coveredCount = (int)cell.aroundCells(virtual).stream().filter(Cell::isCovered).count();
            return cell.value<flaggedCount||cell.value - flaggedCount>coveredCount;
        });
    }
    private void absurdDeductionMining(List<Cell> cells){
        cells.stream().filter(cell->cell.value>0&&cell.value<9).filter(cell->{
            int flaggedCount = (int)cell.aroundCells(cells).stream().filter(Cell::isFlagged).count();
            int coveredCount = (int)cell.aroundCells(cells).stream().filter(Cell::isCovered).count();
            return coveredCount>cell.value - flaggedCount;
        }).forEach(cell->{
            int[] flaggedMap = new int[8];
            int[] removedMap = new int[8];
            int count = (int)tentativeFlagging(cells, cell.index, 0).stream()
                    .peek(AutoMiner::simpleMining)
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
    private static List<Cell> deepCopyOf(List<Cell> original){
        return original.stream().map(Cell::clone).collect(Collectors.toList());
    }
    private static List<List<Cell>> tentativeFlagging(List<Cell> virtual, int baseIndex, int startIndex){
        return virtual.get(baseIndex).aroundCells(virtual).stream()
                .filter(target->target.index>=startIndex)
                .filter(Cell::isCovered)
                .map(target->{
                    List<Cell> child = deepCopyOf(virtual);
                    child.get(target.index).flag();
                    int flaggedCount = (int)virtual.get(baseIndex).aroundCells(child).stream().filter(Cell::isFlagged).count();
                    if(virtual.get(baseIndex).value>flaggedCount){
                        return tentativeFlagging(child, baseIndex, target.index);
                    }else{
                        return Collections.singletonList(child);
                    }
                }).flatMap(List::stream).collect(Collectors.toList());
    }
}
