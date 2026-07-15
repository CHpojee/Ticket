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
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-ink">Specs Viewer</h1>
        <p className="mt-1 text-muted">Browse the feature specifications from docs/specs</p>
      </div>
      {error && <p className="text-rausch">{error}</p>}
      <div className="grid grid-cols-1 gap-6 md:grid-cols-[260px_1fr]">
        <nav data-testid="specs-list" className="card h-fit p-2">
          {specs.map((s) => (
            <button
              type="button"
              key={s.name}
              data-testid={`spec-${s.name}`}
              onClick={() => setSelected(s.name)}
              className={`block w-full rounded-xl px-3.5 py-2.5 text-left text-sm transition-colors ${
                selected === s.name
                  ? 'bg-rausch/10 font-semibold text-rausch'
                  : 'text-ink hover:bg-neutral-100'
              }`}
            >
              {s.title}
            </button>
          ))}
        </nav>
        <article data-testid="spec-content" className="markdown card p-8">
          {content ? (
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{content.markdown}</ReactMarkdown>
          ) : (
            <p className="text-muted">Select a spec to view.</p>
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
