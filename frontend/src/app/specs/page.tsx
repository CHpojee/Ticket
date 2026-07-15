'use client';

import { useEffect, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import Protected from '@/components/Protected';
import { apiFetch } from '@/lib/api';
import type { SpecContent, SpecSummary } from '@/lib/types';

const SpecsContent = () => {
  const [specs, setSpecs] = useState<SpecSummary[]>([]);
  const [selected, setSelected] = useState<string | null>(null);
  const [content, setContent] = useState<SpecContent | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    apiFetch<SpecSummary[]>('/api/specs')
      .then((list) => {
        setSpecs(list);
        if (list.length > 0) setSelected(list[0].name);
      })
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    if (!selected) return;
    apiFetch<SpecContent>(`/api/specs/${selected}`).then(setContent).catch((e) => setError(e.message));
  }, [selected]);

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">Specs Viewer</h1>
      {error && <p className="text-red-600">{error}</p>}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-[240px_1fr]">
        <nav data-testid="specs-list" className="rounded-lg border border-slate-200 bg-white p-2 shadow-sm">
          {specs.map((s) => (
            <button
              type="button"
              key={s.name}
              data-testid={`spec-${s.name}`}
              onClick={() => setSelected(s.name)}
              className={`block w-full rounded px-3 py-2 text-left text-sm hover:bg-slate-100 ${
                selected === s.name ? 'bg-slate-100 font-medium' : ''
              }`}
            >
              {s.title}
            </button>
          ))}
        </nav>
        <article data-testid="spec-content" className="markdown rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          {content ? (
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{content.markdown}</ReactMarkdown>
          ) : (
            <p className="text-slate-400">Select a spec to view.</p>
          )}
        </article>
      </div>
    </div>
  );
};

const SpecsPage = () => (
  <Protected>
    <SpecsContent />
  </Protected>
);

export default SpecsPage;
