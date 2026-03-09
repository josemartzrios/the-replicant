/**
 * MarkdownRenderer — Renders markdown content with syntax highlighting.
 *
 * Uses react-markdown + rehype-highlight for code blocks + remark-gfm for tables.
 * Styled with the Cosmic Neo-Noir theme.
 */

import ReactMarkdown from "react-markdown";
import rehypeHighlight from "rehype-highlight";
import remarkGfm from "remark-gfm";

interface MarkdownRendererProps {
    content: string;
}

export function MarkdownRenderer({ content }: MarkdownRendererProps) {
    return (
        <div className="prose-replicant">
            <ReactMarkdown
                rehypePlugins={[rehypeHighlight]}
                remarkPlugins={[remarkGfm]}
            >
                {content}
            </ReactMarkdown>
        </div>
    );
}
