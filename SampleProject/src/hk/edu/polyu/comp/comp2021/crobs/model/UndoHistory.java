package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.ArrayDeque;
import java.util.Deque;

/** The undo and redo stacks of snapshots for the current login session (BON2). */
final class UndoHistory {

    private final Deque<ModelSnapshot> undoStack = new ArrayDeque<>();
    private final Deque<ModelSnapshot> redoStack = new ArrayDeque<>();

    /**
     * Records the state before a new command. A new command makes the redo history invalid.
     *
     * @param before the state before the command
     */
    void recordChange(ModelSnapshot before) {
        undoStack.push(before);
        redoStack.clear();
    }

    /**
     * Checks whether a command can be undone.
     *
     * @return true if the undo history is not empty
     */
    boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Checks whether an undone command can be redone.
     *
     * @return true if the redo history is not empty
     */
    boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Removes the state to go back to on undo.
     *
     * @return the state before the most recent command
     * @throws IllegalStateException if there is nothing to undo
     */
    ModelSnapshot takeUndo() {
        if (undoStack.isEmpty()) {
            throw new IllegalStateException("Nothing to undo");
        }
        return undoStack.pop();
    }

    /**
     * Removes the state to go forward to on redo.
     *
     * @return the state after the most recently undone command
     * @throws IllegalStateException if there is nothing to redo
     */
    ModelSnapshot takeRedo() {
        if (redoStack.isEmpty()) {
            throw new IllegalStateException("Nothing to redo");
        }
        return redoStack.pop();
    }

    /**
     * Saves the state before an undo so that the command can be redone.
     *
     * @param current the state before the undo
     */
    void pushRedo(ModelSnapshot current) {
        redoStack.push(current);
    }

    /**
     * Saves the state before a redo so that the command can be undone again.
     *
     * @param current the state before the redo
     */
    void pushUndo(ModelSnapshot current) {
        undoStack.push(current);
    }

    /** Forgets all undo and redo history, e.g. when another user logs in. */
    void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
