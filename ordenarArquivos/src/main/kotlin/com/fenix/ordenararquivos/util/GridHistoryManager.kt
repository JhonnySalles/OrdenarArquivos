package com.fenix.ordenararquivos.util

import java.util.ArrayDeque

interface ReversibleAction {
    fun undo()
    fun redo()
    /**
     * Remove referências ao item especificado.
     * Retorna true se a ação se tornar vazia/inválida e deve ser removida da pilha.
     */
    fun removeReferencesTo(item: Any): Boolean
    
    /**
     * Retorna o primeiro item afetado por esta ação (para scroll)
     */
    fun getFirstAffectedItem(): Any?
}

class PropertyChangeAction<T, V>(
    val item: T,
    val oldValue: V,
    val newValue: V,
    val setter: (T, V) -> Unit
) : ReversibleAction {
    override fun undo() {
        setter(item, oldValue)
    }

    override fun redo() {
        setter(item, newValue)
    }

    override fun removeReferencesTo(target: Any): Boolean {
        return item === target
    }

    override fun getFirstAffectedItem(): Any? = item
}

class CompositeAction(val actions: MutableList<ReversibleAction>) : ReversibleAction {
    override fun undo() {
        actions.reversed().forEach { it.undo() }
    }

    override fun redo() {
        actions.forEach { it.redo() }
    }

    override fun removeReferencesTo(target: Any): Boolean {
        actions.removeIf { it.removeReferencesTo(target) }
        return actions.isEmpty()
    }

    override fun getFirstAffectedItem(): Any? = actions.firstOrNull()?.getFirstAffectedItem()
}

class GridHistoryManager {
    private val undoStack = ArrayDeque<ReversibleAction>()
    private val redoStack = ArrayDeque<ReversibleAction>()

    fun pushAction(action: ReversibleAction) {
        undoStack.push(action)
        redoStack.clear()
        // Limitar tamanho do histórico se necessário (ex: 100)
        if (undoStack.size > 100) {
            undoStack.removeLast()
        }
    }

    fun undo(): ReversibleAction? {
        if (undoStack.isEmpty()) return null
        val action = undoStack.pop()
        action.undo()
        redoStack.push(action)
        return action
    }

    fun redo(): ReversibleAction? {
        if (redoStack.isEmpty()) return null
        val action = redoStack.pop()
        action.redo()
        undoStack.push(action)
        return action
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun removeHistoryForItem(item: Any) {
        undoStack.removeIf { it.removeReferencesTo(item) }
        redoStack.removeIf { it.removeReferencesTo(item) }
    }
}
