package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.ast.ElementType.OBJECT_KEYWORD
import com.github.shyiko.ktlint.core.ast.isPartOf
import com.github.shyiko.ktlint.core.ast.isPartOfString
import com.github.shyiko.ktlint.core.ast.nextLeaf
import com.github.shyiko.ktlint.core.ast.prevCodeLeaf
import com.github.shyiko.ktlint.core.ast.prevLeaf
import com.github.shyiko.ktlint.core.ast.upsertWhitespaceAfterMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtEnumEntry

class NoSemicolonsRule : Rule("no-semi") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node is LeafPsiElement && node.textMatches(";") && !node.isPartOfString() &&
                !node.isPartOf(KtEnumEntry::class)) {
            val nextLeaf = node.nextLeaf()
            if (doesNotRequirePreSemi(nextLeaf)) {
                if (node.prevCodeLeaf()?.elementType == OBJECT_KEYWORD) {
                    // https://github.com/shyiko/ktlint/issues/281
                    return
                }
                emit(node.startOffset, "Unnecessary semicolon", true)
                if (autoCorrect) {
                    node.treeParent.removeChild(node)
                }
            } else if (nextLeaf !is PsiWhiteSpace) {
                val prevLeaf = node.prevLeaf()
                if (prevLeaf is PsiWhiteSpace && prevLeaf.textContains('\n')) { // \n;{
                    return
                }
                // todo: move to a separate rule
                emit(node.startOffset + 1, "Missing spacing after \";\"", true)
                if (autoCorrect) {
                    node.upsertWhitespaceAfterMe(" ")
                }
            }
        }
    }

    private fun doesNotRequirePreSemi(nextLeaf: ASTNode?): Boolean {
        if (nextLeaf is PsiWhiteSpace) {
            val nextNextLeaf = nextLeaf.nextLeaf()
            return (
                nextNextLeaf == null || // \s+ and then eof
                nextLeaf.textContains('\n') && nextNextLeaf.text != "{"
            )
        }
        return nextLeaf == null /* eof */
    }
}
