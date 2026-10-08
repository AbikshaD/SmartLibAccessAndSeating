import { apiRequest } from './api';

// Curated fallback book catalog for resilience if backend is offline
export const FALLBACK_BOOKS = [
  // Computer Science
  {
    id: 'cs-1',
    title: 'Clean Code: A Handbook of Agile Software Craftsmanship',
    author: 'Robert C. Martin',
    genre: 'Computer Science',
    isbn: '978-0132350884',
    publicationYear: 2008,
    totalCopies: 5,
    availableCopies: 4,
    shelfLocation: 'Floor 1 - Section CS-01',
    description: 'A handbook of agile software craftsmanship with best practices for writing readable, maintainable, and testable code.'
  },
  {
    id: 'cs-2',
    title: 'Introduction to Algorithms (CLRS)',
    author: 'Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest, Clifford Stein',
    genre: 'Computer Science',
    isbn: '978-0262033848',
    publicationYear: 2009,
    totalCopies: 6,
    availableCopies: 3,
    shelfLocation: 'Floor 1 - Section CS-02',
    description: 'Comprehensive guide covering algorithms analysis, graph theory, dynamic programming, sorting, and data structures.'
  },
  {
    id: 'cs-3',
    title: 'Designing Data-Intensive Applications',
    author: 'Martin Kleppmann',
    genre: 'Computer Science',
    isbn: '978-1449373320',
    publicationYear: 2017,
    totalCopies: 4,
    availableCopies: 2,
    shelfLocation: 'Floor 1 - Section CS-03',
    description: 'The definitive guide to distributed data systems, storage engines, stream processing, reliability, and scalability.'
  },
  {
    id: 'cs-4',
    title: 'The Pragmatic Programmer: Your Journey to Mastery',
    author: 'David Thomas, Andrew Hunt',
    genre: 'Computer Science',
    isbn: '978-0135957059',
    publicationYear: 2019,
    totalCopies: 4,
    availableCopies: 4,
    shelfLocation: 'Floor 1 - Section CS-04',
    description: 'Timeless advice on personal craftsmanship, professional software engineering, debugging, and clean architecture.'
  },
  {
    id: 'cs-5',
    title: 'Artificial Intelligence: A Modern Approach',
    author: 'Stuart Russell, Peter Norvig',
    genre: 'Computer Science',
    isbn: '978-0134610993',
    publicationYear: 2020,
    totalCopies: 5,
    availableCopies: 3,
    shelfLocation: 'Floor 1 - Section CS-05',
    description: 'The leading authoritative textbook on AI theory, machine learning algorithms, probabilistic reasoning, and robotics.'
  },

  // Fiction
  {
    id: 'fic-1',
    title: '1984',
    author: 'George Orwell',
    genre: 'Fiction',
    isbn: '978-0451524935',
    publicationYear: 1949,
    totalCopies: 7,
    availableCopies: 5,
    shelfLocation: 'Floor 2 - Section FIC-01',
    description: 'Classic dystopian masterpiece depicting pervasive state surveillance, totalitarianism, and the battle for individual truth.'
  },
  {
    id: 'fic-2',
    title: 'To Kill a Mockingbird',
    author: 'Harper Lee',
    genre: 'Fiction',
    isbn: '978-0060935467',
    publicationYear: 1960,
    totalCopies: 6,
    availableCopies: 4,
    shelfLocation: 'Floor 2 - Section FIC-02',
    description: 'Pulitzer Prize-winning tale of justice, childhood innocence, racial injustice, and compassion in the American South.'
  },
  {
    id: 'fic-3',
    title: 'The Great Gatsby',
    author: 'F. Scott Fitzgerald',
    genre: 'Fiction',
    isbn: '978-0743273565',
    publicationYear: 1925,
    totalCopies: 5,
    availableCopies: 3,
    shelfLocation: 'Floor 2 - Section FIC-03',
    description: 'Exquisite tragedy capturing obsession, wealth, love, illusion, and moral decay during the Roaring Twenties.'
  },
  {
    id: 'fic-4',
    title: 'Brave New World',
    author: 'Aldous Huxley',
    genre: 'Fiction',
    isbn: '978-0060850524',
    publicationYear: 1932,
    totalCopies: 4,
    availableCopies: 2,
    shelfLocation: 'Floor 2 - Section FIC-04',
    description: 'Visionary dystopian satire illustrating biological conditioning, pharmacological escapism, and consumerist conformity.'
  },
  {
    id: 'fic-5',
    title: 'The Alchemist',
    author: 'Paulo Coelho',
    genre: 'Fiction',
    isbn: '978-0062315007',
    publicationYear: 1988,
    totalCopies: 8,
    availableCopies: 6,
    shelfLocation: 'Floor 2 - Section FIC-05',
    description: 'Enchanting fable following Santiago, an Andalusian shepherd boy journeying to the pyramids to realize his personal legend.'
  },

  // Science & Mathematics
  {
    id: 'sci-1',
    title: 'A Brief History of Time',
    author: 'Stephen Hawking',
    genre: 'Science & Mathematics',
    isbn: '978-0553380163',
    publicationYear: 1988,
    totalCopies: 5,
    availableCopies: 3,
    shelfLocation: 'Floor 1 - Section SCI-01',
    description: 'Landmark overview of cosmology, black holes, general relativity, quantum mechanics, and the origins of space and time.'
  },
  {
    id: 'sci-2',
    title: 'Cosmos',
    author: 'Carl Sagan',
    genre: 'Science & Mathematics',
    isbn: '978-0345539434',
    publicationYear: 1980,
    totalCopies: 4,
    availableCopies: 3,
    shelfLocation: 'Floor 1 - Section SCI-02',
    description: 'Celebrated exploration of the universe, scientific history, planetary evolution, and humanity cosmic kinship.'
  },
  {
    id: 'sci-3',
    title: 'Calculus: Early Transcendentals',
    author: 'James Stewart',
    genre: 'Science & Mathematics',
    isbn: '978-1285741550',
    publicationYear: 2015,
    totalCopies: 6,
    availableCopies: 5,
    shelfLocation: 'Floor 1 - Section SCI-03',
    description: 'Definitive collegiate calculus textbook covering differential and integral calculus, series, and vector analysis.'
  },
  {
    id: 'sci-4',
    title: 'The Selfish Gene',
    author: 'Richard Dawkins',
    genre: 'Science & Mathematics',
    isbn: '978-0198788607',
    publicationYear: 1976,
    totalCopies: 4,
    availableCopies: 2,
    shelfLocation: 'Floor 1 - Section SCI-04',
    description: 'Pioneering work demonstrating the gene-centric view of evolution and coining the concept of cultural memes.'
  },

  // History & Philosophy
  {
    id: 'his-1',
    title: 'Sapiens: A Brief History of Humankind',
    author: 'Yuval Noah Harari',
    genre: 'History & Philosophy',
    isbn: '978-0062316097',
    publicationYear: 2015,
    totalCopies: 6,
    availableCopies: 4,
    shelfLocation: 'Floor 2 - Section HIS-01',
    description: 'Sweeping narrative detailing human evolution from stone-age foragers to the masters of planet Earth.'
  },
  {
    id: 'his-2',
    title: 'Meditations',
    author: 'Marcus Aurelius',
    genre: 'History & Philosophy',
    isbn: '978-0812968255',
    publicationYear: 2002,
    totalCopies: 5,
    availableCopies: 4,
    shelfLocation: 'Floor 2 - Section HIS-02',
    description: 'Intimate spiritual journal and timeless Stoic principles from the Roman Emperor on resilience, duty, and virtue.'
  },
  {
    id: 'his-3',
    title: 'Guns, Germs, and Steel',
    author: 'Jared Diamond',
    genre: 'History & Philosophy',
    isbn: '978-0393354324',
    publicationYear: 1997,
    totalCopies: 4,
    availableCopies: 2,
    shelfLocation: 'Floor 2 - Section HIS-03',
    description: 'Pulitzer Prize-winning analysis of how environmental geography shaped human societies and technological disparities.'
  },
  {
    id: 'his-4',
    title: 'The Republic',
    author: 'Plato',
    genre: 'History & Philosophy',
    isbn: '978-0140455113',
    publicationYear: 2007,
    totalCopies: 4,
    availableCopies: 3,
    shelfLocation: 'Floor 2 - Section HIS-04',
    description: 'Foundational philosophical dialogue exploring justice, the philosopher king, the allegory of the cave, and governance.'
  },

  // Business & Management
  {
    id: 'bus-1',
    title: 'The Lean Startup',
    author: 'Eric Ries',
    genre: 'Business & Management',
    isbn: '978-0307887894',
    publicationYear: 2011,
    totalCopies: 5,
    availableCopies: 4,
    shelfLocation: 'Floor 3 - Section BUS-01',
    description: 'Revolutionary blueprint for building businesses through continuous experimentation, minimum viable products, and validated learning.'
  },
  {
    id: 'bus-2',
    title: 'Zero to One: Notes on Startups',
    author: 'Peter Thiel, Blake Masters',
    genre: 'Business & Management',
    isbn: '978-0770436774',
    publicationYear: 2014,
    totalCopies: 6,
    availableCopies: 5,
    shelfLocation: 'Floor 3 - Section BUS-02',
    description: 'Provocative guide on escaping competition, building valuable monopolies, and creating singular new inventions.'
  },
  {
    id: 'bus-3',
    title: 'Thinking, Fast and Slow',
    author: 'Daniel Kahneman',
    genre: 'Business & Management',
    isbn: '978-0374533557',
    publicationYear: 2011,
    totalCopies: 5,
    availableCopies: 3,
    shelfLocation: 'Floor 3 - Section BUS-03',
    description: 'Masterwork by the Nobel Laureate dissecting System 1 intuition versus System 2 deliberate thinking and cognitive biases.'
  },
  {
    id: 'bus-4',
    title: 'Principles: Life and Work',
    author: 'Ray Dalio',
    genre: 'Business & Management',
    isbn: '978-1501124020',
    publicationYear: 2017,
    totalCopies: 4,
    availableCopies: 3,
    shelfLocation: 'Floor 3 - Section BUS-04',
    description: 'Actionable life and work principles centered on radical truth, radical transparency, and thoughtful disagreement.'
  },

  // Literature & Classics
  {
    id: 'lit-1',
    title: 'Pride and Prejudice',
    author: 'Jane Austen',
    genre: 'Literature & Classics',
    isbn: '978-0141439518',
    publicationYear: 1813,
    totalCopies: 5,
    availableCopies: 4,
    shelfLocation: 'Floor 2 - Section LIT-01',
    description: 'Beloved romantic classic exploring class, societal expectations, first impressions, and matrimonial motives.'
  },
  {
    id: 'lit-2',
    title: 'Crime and Punishment',
    author: 'Fyodor Dostoevsky',
    genre: 'Literature & Classics',
    isbn: '978-0143058144',
    publicationYear: 1866,
    totalCopies: 4,
    availableCopies: 3,
    shelfLocation: 'Floor 2 - Section LIT-02',
    description: 'Psychological examination of morality, guilt, suffering, and redemption through the journey of Rodion Raskolnikov.'
  },
  {
    id: 'lit-3',
    title: 'The Odyssey',
    author: 'Homer',
    genre: 'Literature & Classics',
    isbn: '978-0140268866',
    publicationYear: 1996,
    totalCopies: 4,
    availableCopies: 2,
    shelfLocation: 'Floor 2 - Section LIT-03',
    description: 'Epic Greek poetry recounting Odysseus perilous decade-long journey home to Ithaca following the Trojan War.'
  }
];

export const bookApi = {
  async getAllBooks(genre) {
    try {
      const path = genre ? `/books?genre=${encodeURIComponent(genre)}` : '/books';
      const data = await apiRequest(path);
      if (Array.isArray(data) && data.length > 0) {
        return data;
      }
    } catch {
      // Backend not running or error, fallback below
    }

    if (genre) {
      return FALLBACK_BOOKS.filter(
        (b) => b.genre.toLowerCase() === genre.toLowerCase()
      );
    }
    return FALLBACK_BOOKS;
  },

  async getGenres() {
    try {
      const data = await apiRequest('/books/genres');
      if (Array.isArray(data) && data.length > 0) {
        return data;
      }
    } catch {
      // Backend not running or error, fallback below
    }

    // Compute genre stats from fallback
    const genreMap = {};
    for (const b of FALLBACK_BOOKS) {
      if (!genreMap[b.genre]) {
        genreMap[b.genre] = { genre: b.genre, bookCount: 0, availableCopies: 0, totalCopies: 0 };
      }
      genreMap[b.genre].bookCount++;
      genreMap[b.genre].availableCopies += b.availableCopies;
      genreMap[b.genre].totalCopies += b.totalCopies;
    }
    return Object.values(genreMap);
  },

  async getBooksByGenre(genre) {
    try {
      const data = await apiRequest(`/books/genre/${encodeURIComponent(genre)}`);
      if (data && Array.isArray(data.books)) {
        return data;
      }
    } catch {
      // Backend not running or error, fallback below
    }

    const filtered = FALLBACK_BOOKS.filter(
      (b) => b.genre.toLowerCase() === genre.toLowerCase()
    );
    return {
      genre,
      bookCount: filtered.length,
      books: filtered
    };
  }
};
