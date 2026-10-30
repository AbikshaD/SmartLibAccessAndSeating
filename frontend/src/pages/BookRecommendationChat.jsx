import { useEffect, useRef, useState } from 'react';
import { AlertCircle, Bot, BookOpen, RotateCcw, Send, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';
import { bookApi } from '../api/bookApi';

const starterPrompts = [
  'Suggest beginner-level Java books',
  'What do you have about machine learning?',
  'Recommend books about history and philosophy',
];

const getAssistantContext = (message) => {
  const books = message.books || [];
  if (books.length === 0) return message.content;
  const bookDetails = books.map((book) => (
    `"${book.title}" by ${book.author} (${book.genre})`
  )).join('; ');
  return `${message.content}\nBooks returned from the library catalog: ${bookDetails}`;
};

const getAvailableCopiesLabel = (book) => {
  if (book.availableCopies == null) return 'Availability not listed';
  if (book.availableCopies === 0) return 'Currently unavailable';
  return `${book.availableCopies} ${book.availableCopies === 1 ? 'copy' : 'copies'} available`;
};

export default function BookRecommendationChat() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const transcriptEndRef = useRef(null);

  useEffect(() => {
    transcriptEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, sending]);

  const sendMessage = async (event) => {
    event.preventDefault();
    const text = input.trim();
    if (!text || sending) return;

    setInput('');
    setError('');
    setSending(true);
    try {
      const conversation = messages.slice(-10).map((message) => ({
        role: message.role,
        content: message.role === 'assistant' ? getAssistantContext(message) : message.content,
      }));
      const response = await bookApi.getRecommendations(text, conversation);
      setMessages((current) => [
        ...current,
        { id: `${Date.now()}-user`, role: 'user', content: text },
        {
          id: `${Date.now()}-assistant`,
          role: 'assistant',
          content: response.message,
          books: response.books || [],
        },
      ]);
    } catch (requestError) {
      setInput(text);
      setError(requestError.message || 'Unable to get book recommendations. Please try again.');
    } finally {
      setSending(false);
    }
  };

  const resetConversation = () => {
    if (sending) return;
    setMessages([]);
    setInput('');
    setError('');
  };

  return (
    <div className="page-container recommendation-page">
      <header className="page-header recommendation-page-header">
        <div>
          <p className="eyebrow">AI book assistant</p>
          <h1>Find your next read</h1>
          <p className="page-subtitle">
            Ask about a subject, genre, author, or a book you enjoyed. Recommendations come from our library catalog.
          </p>
        </div>
        {messages.length > 0 && (
          <button
            type="button"
            className="secondary recommendation-reset"
            onClick={resetConversation}
            disabled={sending}
          >
            <RotateCcw size={16} />
            New conversation
          </button>
        )}
      </header>

      <section className="recommendation-chat panel-card" aria-label="Book recommendation chat">
        <div className="recommendation-chat-heading">
          <span className="recommendation-avatar"><Bot size={19} /></span>
          <div>
            <strong>SmartLib Book Assistant</strong>
            <span><span className="recommendation-online-dot" /> Recommends books in this library</span>
          </div>
        </div>

        <div className="recommendation-transcript" aria-live="polite" aria-relevant="additions text">
          {messages.length === 0 && (
            <div className="recommendation-welcome">
              <span className="recommendation-welcome-icon"><Sparkles size={22} /></span>
              <h2>What would you like to read?</h2>
              <p>Tell me what you’re interested in, and I’ll look for matching books in the library collection.</p>
              <div className="recommendation-starters">
                {starterPrompts.map((prompt) => (
                  <button
                    type="button"
                    key={prompt}
                    onClick={() => setInput(prompt)}
                    disabled={sending}
                  >
                    {prompt}
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((message) => (
            <article
              className={`recommendation-message recommendation-message-${message.role}`}
              key={message.id}
            >
              {message.role === 'assistant' && (
                <span className="recommendation-message-avatar"><Bot size={16} /></span>
              )}
              <div className="recommendation-message-body">
                <p>{message.content}</p>
                {message.books?.length > 0 && (
                  <div className="recommendation-book-list">
                    {message.books.map((book) => (
                      <article className="recommendation-book-card" key={book.id}>
                        <div className="recommendation-book-title-row">
                          <span className="recommendation-book-icon"><BookOpen size={17} /></span>
                          <div>
                            <h3>{book.title}</h3>
                            <p>{book.author}</p>
                          </div>
                        </div>
                        {book.description && (
                          <p className="recommendation-book-description">{book.description}</p>
                        )}
                        <div className="recommendation-book-meta">
                          {book.genre && <span>{book.genre}</span>}
                          {book.publicationYear && <span>{book.publicationYear}</span>}
                          <span className={book.availableCopies > 0
                            ? 'recommendation-availability'
                            : 'recommendation-unavailable'}
                          >
                            {getAvailableCopiesLabel(book)}
                          </span>
                          {book.shelfLocation && <span>{book.shelfLocation}</span>}
                        </div>
                      </article>
                    ))}
                  </div>
                )}
              </div>
            </article>
          ))}

          {sending && (
            <div className="recommendation-message recommendation-message-assistant" role="status">
              <span className="recommendation-message-avatar"><Bot size={16} /></span>
              <div className="recommendation-thinking">
                <span />
                <span />
                <span />
                <span className="sr-only">Searching the library catalog…</span>
              </div>
            </div>
          )}
          <div ref={transcriptEndRef} />
        </div>

        {error && (
          <div className="recommendation-error" role="alert">
            <AlertCircle size={17} />
            <span>{error}</span>
          </div>
        )}

        <form className="recommendation-composer" onSubmit={sendMessage}>
          <label className="sr-only" htmlFor="recommendation-message">Ask for a book recommendation</label>
          <textarea
            id="recommendation-message"
            value={input}
            onChange={(event) => setInput(event.target.value)}
            placeholder="Ask for a recommendation…"
            maxLength={1000}
            rows={2}
            disabled={sending}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                event.currentTarget.form.requestSubmit();
              }
            }}
          />
          <div className="recommendation-composer-footer">
            <span>Shift + Enter for a new line · {input.length}/1000</span>
            <button type="submit" className="primary-button" disabled={sending || !input.trim()}>
              <Send size={16} />
              {sending ? 'Searching…' : 'Send'}
            </button>
          </div>
        </form>
      </section>

      <p className="recommendation-disclaimer">
        Recommendations are limited to books in the catalog. <Link to="/student/books">Browse all books</Link>
      </p>
    </div>
  );
}
