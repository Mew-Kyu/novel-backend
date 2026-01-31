--
-- PostgreSQL database dump
--

\restrict ccwRqpVhAR7eS8iYdVTU1ppZrqLYnZFlgcoe5HyS6CYmfXYBYMFgHhq5IshD3kY

-- Dumped from database version 18.1 (Debian 18.1-1.pgdg12+2)
-- Dumped by pg_dump version 18.0

-- Started on 2026-01-29 22:42:53

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 2 (class 3079 OID 16421)
-- Name: vector; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;


--
-- TOC entry 3852 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION vector; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION vector IS 'vector data type and ivfflat and hnsw access methods';


--
-- TOC entry 307 (class 1255 OID 163886)
-- Name: update_user_profile_timestamp(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_user_profile_timestamp() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_user_profile_timestamp() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 237 (class 1259 OID 57406)
-- Name: comments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.comments (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    story_id bigint NOT NULL,
    content text NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.comments OWNER TO postgres;

--
-- TOC entry 3853 (class 0 OID 0)
-- Dependencies: 237
-- Name: TABLE comments; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.comments IS 'User comments and reviews for stories';


--
-- TOC entry 236 (class 1259 OID 57405)
-- Name: comments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.comments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.comments_id_seq OWNER TO postgres;

--
-- TOC entry 3854 (class 0 OID 0)
-- Dependencies: 236
-- Name: comments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.comments_id_seq OWNED BY public.comments.id;


--
-- TOC entry 229 (class 1259 OID 32863)
-- Name: crawl_jobs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.crawl_jobs (
    id integer NOT NULL,
    story_id integer,
    chapter_id integer,
    job_type character varying(50) NOT NULL,
    status character varying(50) DEFAULT 'PENDING'::character varying NOT NULL,
    attempts integer DEFAULT 0,
    error_message text,
    created_at timestamp without time zone DEFAULT now(),
    updated_at timestamp without time zone,
    created_by bigint,
    last_modified_by bigint
);


ALTER TABLE public.crawl_jobs OWNER TO postgres;

--
-- TOC entry 228 (class 1259 OID 32862)
-- Name: crawl_jobs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.crawl_jobs_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.crawl_jobs_id_seq OWNER TO postgres;

--
-- TOC entry 3855 (class 0 OID 0)
-- Dependencies: 228
-- Name: crawl_jobs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.crawl_jobs_id_seq OWNED BY public.crawl_jobs.id;


--
-- TOC entry 241 (class 1259 OID 73730)
-- Name: favorites; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.favorites (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    story_id bigint NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.favorites OWNER TO postgres;

--
-- TOC entry 3856 (class 0 OID 0)
-- Dependencies: 241
-- Name: TABLE favorites; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.favorites IS 'User favorite/bookmarked stories';


--
-- TOC entry 240 (class 1259 OID 73729)
-- Name: favorites_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.favorites_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.favorites_id_seq OWNER TO postgres;

--
-- TOC entry 3857 (class 0 OID 0)
-- Dependencies: 240
-- Name: favorites_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.favorites_id_seq OWNED BY public.favorites.id;


--
-- TOC entry 230 (class 1259 OID 32889)
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- TOC entry 232 (class 1259 OID 57346)
-- Name: genres; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.genres (
    id bigint NOT NULL,
    name character varying(50) NOT NULL,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.genres OWNER TO postgres;

--
-- TOC entry 3858 (class 0 OID 0)
-- Dependencies: 232
-- Name: TABLE genres; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.genres IS 'Available genres for categorizing stories';


--
-- TOC entry 231 (class 1259 OID 57345)
-- Name: genres_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.genres_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.genres_id_seq OWNER TO postgres;

--
-- TOC entry 3859 (class 0 OID 0)
-- Dependencies: 231
-- Name: genres_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.genres_id_seq OWNED BY public.genres.id;


--
-- TOC entry 243 (class 1259 OID 122882)
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.password_reset_tokens (
    id bigint NOT NULL,
    token character varying(255) NOT NULL,
    user_id bigint NOT NULL,
    expiry_date timestamp without time zone NOT NULL,
    used boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.password_reset_tokens OWNER TO postgres;

--
-- TOC entry 242 (class 1259 OID 122881)
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.password_reset_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.password_reset_tokens_id_seq OWNER TO postgres;

--
-- TOC entry 3860 (class 0 OID 0)
-- Dependencies: 242
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.password_reset_tokens_id_seq OWNED BY public.password_reset_tokens.id;


--
-- TOC entry 235 (class 1259 OID 57378)
-- Name: ratings; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ratings (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    story_id bigint NOT NULL,
    rating integer NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ratings_rating_check CHECK (((rating >= 1) AND (rating <= 5)))
);


ALTER TABLE public.ratings OWNER TO postgres;

--
-- TOC entry 3861 (class 0 OID 0)
-- Dependencies: 235
-- Name: TABLE ratings; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.ratings IS 'User ratings for stories (1-5 stars)';


--
-- TOC entry 3862 (class 0 OID 0)
-- Dependencies: 235
-- Name: COLUMN ratings.rating; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.ratings.rating IS 'Rating value from 1 to 5 stars';


--
-- TOC entry 234 (class 1259 OID 57377)
-- Name: ratings_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ratings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.ratings_id_seq OWNER TO postgres;

--
-- TOC entry 3863 (class 0 OID 0)
-- Dependencies: 234
-- Name: ratings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.ratings_id_seq OWNED BY public.ratings.id;


--
-- TOC entry 239 (class 1259 OID 65538)
-- Name: roles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.roles (
    id bigint NOT NULL,
    name character varying(50) NOT NULL,
    description character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.roles OWNER TO postgres;

--
-- TOC entry 238 (class 1259 OID 65537)
-- Name: roles_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.roles_id_seq OWNER TO postgres;

--
-- TOC entry 3864 (class 0 OID 0)
-- Dependencies: 238
-- Name: roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.roles_id_seq OWNED BY public.roles.id;


--
-- TOC entry 223 (class 1259 OID 32801)
-- Name: stories; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.stories (
    id integer NOT NULL,
    title text NOT NULL,
    author_name text,
    description text,
    cover_image_url text,
    source_url text,
    source_site character varying(255) DEFAULT 'syosetu'::character varying,
    created_at timestamp without time zone DEFAULT now(),
    embedding public.vector(768),
    raw_title text,
    raw_description text,
    translated_title text,
    translated_description text,
    raw_author_name text,
    translated_author_name text,
    updated_at timestamp without time zone,
    view_count bigint DEFAULT 0,
    featured boolean DEFAULT false,
    status character varying(20) DEFAULT 'PUBLISHED'::character varying,
    average_rating numeric(3,1) DEFAULT NULL::numeric,
    total_ratings bigint DEFAULT 0,
    created_by bigint,
    last_modified_by bigint
);


ALTER TABLE public.stories OWNER TO postgres;

--
-- TOC entry 3865 (class 0 OID 0)
-- Dependencies: 223
-- Name: COLUMN stories.raw_title; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.stories.raw_title IS 'Original title from crawled source (Japanese)';


--
-- TOC entry 3866 (class 0 OID 0)
-- Dependencies: 223
-- Name: COLUMN stories.raw_description; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.stories.raw_description IS 'Original description from crawled source (Japanese)';


--
-- TOC entry 3867 (class 0 OID 0)
-- Dependencies: 223
-- Name: COLUMN stories.translated_title; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.stories.translated_title IS 'Translated title (Vietnamese)';


--
-- TOC entry 3868 (class 0 OID 0)
-- Dependencies: 223
-- Name: COLUMN stories.translated_description; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.stories.translated_description IS 'Translated description (Vietnamese)';


--
-- TOC entry 3869 (class 0 OID 0)
-- Dependencies: 223
-- Name: COLUMN stories.raw_author_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.stories.raw_author_name IS 'Original author name from crawled source (Japanese)';


--
-- TOC entry 3870 (class 0 OID 0)
-- Dependencies: 223
-- Name: COLUMN stories.translated_author_name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.stories.translated_author_name IS 'Translated author name (Vietnamese)';


--
-- TOC entry 3871 (class 0 OID 0)
-- Dependencies: 223
-- Name: COLUMN stories.updated_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.stories.updated_at IS 'Last update time of the story';


--
-- TOC entry 3872 (class 0 OID 0)
-- Dependencies: 223
-- Name: COLUMN stories.view_count; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.stories.view_count IS 'Total view count for the story';


--
-- TOC entry 3873 (class 0 OID 0)
-- Dependencies: 223
-- Name: COLUMN stories.featured; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.stories.featured IS 'Whether the story is featured on homepage';


--
-- TOC entry 222 (class 1259 OID 32800)
-- Name: stories_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.stories_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.stories_id_seq OWNER TO postgres;

--
-- TOC entry 3874 (class 0 OID 0)
-- Dependencies: 222
-- Name: stories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.stories_id_seq OWNED BY public.stories.id;


--
-- TOC entry 225 (class 1259 OID 32816)
-- Name: story_chapters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.story_chapters (
    id integer NOT NULL,
    story_id integer,
    chapter_index integer NOT NULL,
    title text,
    raw_content text,
    crawl_status character varying(50) DEFAULT 'PENDING'::character varying,
    crawl_time timestamp without time zone,
    translated_content text,
    translate_status character varying(50) DEFAULT 'NONE'::character varying,
    translate_time timestamp without time zone,
    created_at timestamp without time zone DEFAULT now(),
    raw_title text,
    translated_title text,
    updated_at timestamp without time zone,
    created_by bigint,
    last_modified_by bigint
);


ALTER TABLE public.story_chapters OWNER TO postgres;

--
-- TOC entry 3875 (class 0 OID 0)
-- Dependencies: 225
-- Name: COLUMN story_chapters.raw_title; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.story_chapters.raw_title IS 'Original chapter title from crawled source (Japanese)';


--
-- TOC entry 3876 (class 0 OID 0)
-- Dependencies: 225
-- Name: COLUMN story_chapters.translated_title; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.story_chapters.translated_title IS 'Translated chapter title (Vietnamese)';


--
-- TOC entry 3877 (class 0 OID 0)
-- Dependencies: 225
-- Name: COLUMN story_chapters.updated_at; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.story_chapters.updated_at IS 'Last update time of the chapter';


--
-- TOC entry 224 (class 1259 OID 32815)
-- Name: story_chapters_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.story_chapters_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.story_chapters_id_seq OWNER TO postgres;

--
-- TOC entry 3878 (class 0 OID 0)
-- Dependencies: 224
-- Name: story_chapters_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.story_chapters_id_seq OWNED BY public.story_chapters.id;


--
-- TOC entry 233 (class 1259 OID 57360)
-- Name: story_genres; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.story_genres (
    story_id bigint NOT NULL,
    genre_id bigint NOT NULL
);


ALTER TABLE public.story_genres OWNER TO postgres;

--
-- TOC entry 3879 (class 0 OID 0)
-- Dependencies: 233
-- Name: TABLE story_genres; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.story_genres IS 'Many-to-many relationship between stories and genres';


--
-- TOC entry 227 (class 1259 OID 32837)
-- Name: user_history_read; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_history_read (
    id integer NOT NULL,
    user_id integer,
    story_id integer,
    chapter_id integer,
    progress_percent integer DEFAULT 0,
    scroll_offset integer DEFAULT 0,
    last_read_at timestamp without time zone DEFAULT now()
);


ALTER TABLE public.user_history_read OWNER TO postgres;

--
-- TOC entry 226 (class 1259 OID 32836)
-- Name: user_history_read_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_history_read_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_history_read_id_seq OWNER TO postgres;

--
-- TOC entry 3880 (class 0 OID 0)
-- Dependencies: 226
-- Name: user_history_read_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_history_read_id_seq OWNED BY public.user_history_read.id;


--
-- TOC entry 247 (class 1259 OID 163889)
-- Name: user_onboarding; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_onboarding (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    preferred_genres text,
    reading_frequency character varying(50),
    preferred_length character varying(50),
    completion_preference character varying(50),
    exploration_preference character varying(50),
    completed boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.user_onboarding OWNER TO postgres;

--
-- TOC entry 3881 (class 0 OID 0)
-- Dependencies: 247
-- Name: TABLE user_onboarding; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.user_onboarding IS 'User preferences collected during first-time onboarding';


--
-- TOC entry 3882 (class 0 OID 0)
-- Dependencies: 247
-- Name: COLUMN user_onboarding.preferred_genres; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.user_onboarding.preferred_genres IS 'Comma-separated list of preferred genre IDs';


--
-- TOC entry 3883 (class 0 OID 0)
-- Dependencies: 247
-- Name: COLUMN user_onboarding.reading_frequency; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.user_onboarding.reading_frequency IS 'How often user reads: DAILY, WEEKLY, MONTHLY, CASUAL';


--
-- TOC entry 3884 (class 0 OID 0)
-- Dependencies: 247
-- Name: COLUMN user_onboarding.exploration_preference; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.user_onboarding.exploration_preference IS 'SAFE (known genres), ADVENTUROUS (try new), BALANCED';


--
-- TOC entry 246 (class 1259 OID 163888)
-- Name: user_onboarding_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_onboarding_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_onboarding_id_seq OWNER TO postgres;

--
-- TOC entry 3885 (class 0 OID 0)
-- Dependencies: 246
-- Name: user_onboarding_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_onboarding_id_seq OWNED BY public.user_onboarding.id;


--
-- TOC entry 245 (class 1259 OID 163854)
-- Name: user_profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_profiles (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    profile_embedding public.vector(768),
    total_stories_read integer DEFAULT 0,
    total_chapters_read integer DEFAULT 0,
    average_completion_rate numeric(5,2) DEFAULT 0.0,
    chapters_per_week numeric(10,2) DEFAULT 0.0,
    avg_session_duration_minutes numeric(10,2) DEFAULT 0.0,
    genre_diversity_score numeric(5,2) DEFAULT 0.0,
    last_profile_update timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.user_profiles OWNER TO postgres;

--
-- TOC entry 3886 (class 0 OID 0)
-- Dependencies: 245
-- Name: TABLE user_profiles; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.user_profiles IS 'Stores aggregated user behavior and preferences for enhanced recommendations';


--
-- TOC entry 3887 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN user_profiles.profile_embedding; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.user_profiles.profile_embedding IS 'Weighted average of embeddings from stories user has interacted with';


--
-- TOC entry 3888 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN user_profiles.average_completion_rate; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.user_profiles.average_completion_rate IS 'Percentage of started stories that user completed (progress >= 90%)';


--
-- TOC entry 3889 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN user_profiles.chapters_per_week; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.user_profiles.chapters_per_week IS 'Average reading velocity in recent 30 days';


--
-- TOC entry 3890 (class 0 OID 0)
-- Dependencies: 245
-- Name: COLUMN user_profiles.genre_diversity_score; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.user_profiles.genre_diversity_score IS 'Measures how diverse users reading taste is (0=narrow, 1=very diverse)';


--
-- TOC entry 244 (class 1259 OID 163853)
-- Name: user_profiles_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.user_profiles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.user_profiles_id_seq OWNER TO postgres;

--
-- TOC entry 3891 (class 0 OID 0)
-- Dependencies: 244
-- Name: user_profiles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.user_profiles_id_seq OWNED BY public.user_profiles.id;


--
-- TOC entry 221 (class 1259 OID 32785)
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id integer NOT NULL,
    email character varying(255) NOT NULL,
    password_hash text NOT NULL,
    display_name character varying(255) NOT NULL,
    created_at timestamp without time zone DEFAULT now(),
    active boolean DEFAULT true NOT NULL,
    avatar_url text,
    role_id bigint NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- TOC entry 3892 (class 0 OID 0)
-- Dependencies: 221
-- Name: COLUMN users.avatar_url; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.users.avatar_url IS 'URL to user avatar image stored in Cloudinary';


--
-- TOC entry 220 (class 1259 OID 32784)
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO postgres;

--
-- TOC entry 3893 (class 0 OID 0)
-- Dependencies: 220
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- TOC entry 3561 (class 2604 OID 57409)
-- Name: comments id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments ALTER COLUMN id SET DEFAULT nextval('public.comments_id_seq'::regclass);


--
-- TOC entry 3551 (class 2604 OID 32866)
-- Name: crawl_jobs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.crawl_jobs ALTER COLUMN id SET DEFAULT nextval('public.crawl_jobs_id_seq'::regclass);


--
-- TOC entry 3566 (class 2604 OID 73733)
-- Name: favorites id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.favorites ALTER COLUMN id SET DEFAULT nextval('public.favorites_id_seq'::regclass);


--
-- TOC entry 3556 (class 2604 OID 57349)
-- Name: genres id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.genres ALTER COLUMN id SET DEFAULT nextval('public.genres_id_seq'::regclass);


--
-- TOC entry 3568 (class 2604 OID 122885)
-- Name: password_reset_tokens id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens ALTER COLUMN id SET DEFAULT nextval('public.password_reset_tokens_id_seq'::regclass);


--
-- TOC entry 3558 (class 2604 OID 57381)
-- Name: ratings id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ratings ALTER COLUMN id SET DEFAULT nextval('public.ratings_id_seq'::regclass);


--
-- TOC entry 3564 (class 2604 OID 65541)
-- Name: roles id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles ALTER COLUMN id SET DEFAULT nextval('public.roles_id_seq'::regclass);


--
-- TOC entry 3535 (class 2604 OID 32804)
-- Name: stories id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.stories ALTER COLUMN id SET DEFAULT nextval('public.stories_id_seq'::regclass);


--
-- TOC entry 3543 (class 2604 OID 32819)
-- Name: story_chapters id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.story_chapters ALTER COLUMN id SET DEFAULT nextval('public.story_chapters_id_seq'::regclass);


--
-- TOC entry 3547 (class 2604 OID 32840)
-- Name: user_history_read id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_history_read ALTER COLUMN id SET DEFAULT nextval('public.user_history_read_id_seq'::regclass);


--
-- TOC entry 3581 (class 2604 OID 163892)
-- Name: user_onboarding id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_onboarding ALTER COLUMN id SET DEFAULT nextval('public.user_onboarding_id_seq'::regclass);


--
-- TOC entry 3571 (class 2604 OID 163857)
-- Name: user_profiles id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles ALTER COLUMN id SET DEFAULT nextval('public.user_profiles_id_seq'::regclass);


--
-- TOC entry 3532 (class 2604 OID 32788)
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- TOC entry 3635 (class 2606 OID 57421)
-- Name: comments comments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);


--
-- TOC entry 3613 (class 2606 OID 32876)
-- Name: crawl_jobs crawl_jobs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.crawl_jobs
    ADD CONSTRAINT crawl_jobs_pkey PRIMARY KEY (id);


--
-- TOC entry 3645 (class 2606 OID 73740)
-- Name: favorites favorites_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT favorites_pkey PRIMARY KEY (id);


--
-- TOC entry 3617 (class 2606 OID 32904)
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- TOC entry 3620 (class 2606 OID 57359)
-- Name: genres genres_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.genres
    ADD CONSTRAINT genres_name_key UNIQUE (name);


--
-- TOC entry 3622 (class 2606 OID 57357)
-- Name: genres genres_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.genres
    ADD CONSTRAINT genres_pkey PRIMARY KEY (id);


--
-- TOC entry 3655 (class 2606 OID 122895)
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- TOC entry 3657 (class 2606 OID 122897)
-- Name: password_reset_tokens password_reset_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_token_key UNIQUE (token);


--
-- TOC entry 3631 (class 2606 OID 57392)
-- Name: ratings ratings_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT ratings_pkey PRIMARY KEY (id);


--
-- TOC entry 3641 (class 2606 OID 65549)
-- Name: roles roles_name_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_name_key UNIQUE (name);


--
-- TOC entry 3643 (class 2606 OID 65547)
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- TOC entry 3600 (class 2606 OID 32812)
-- Name: stories stories_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.stories
    ADD CONSTRAINT stories_pkey PRIMARY KEY (id);


--
-- TOC entry 3605 (class 2606 OID 32828)
-- Name: story_chapters story_chapters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.story_chapters
    ADD CONSTRAINT story_chapters_pkey PRIMARY KEY (id);


--
-- TOC entry 3626 (class 2606 OID 57366)
-- Name: story_genres story_genres_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.story_genres
    ADD CONSTRAINT story_genres_pkey PRIMARY KEY (story_id, genre_id);


--
-- TOC entry 3650 (class 2606 OID 73742)
-- Name: favorites uk_user_story_favorite; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT uk_user_story_favorite UNIQUE (user_id, story_id);


--
-- TOC entry 3894 (class 0 OID 0)
-- Dependencies: 3650
-- Name: CONSTRAINT uk_user_story_favorite ON favorites; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON CONSTRAINT uk_user_story_favorite ON public.favorites IS 'A user can only favorite a story once';


--
-- TOC entry 3609 (class 2606 OID 114690)
-- Name: user_history_read uk_user_story_history; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_history_read
    ADD CONSTRAINT uk_user_story_history UNIQUE (user_id, story_id);


--
-- TOC entry 3895 (class 0 OID 0)
-- Dependencies: 3609
-- Name: CONSTRAINT uk_user_story_history ON user_history_read; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON CONSTRAINT uk_user_story_history ON public.user_history_read IS 'Ensures each user can only have one reading history record per story';


--
-- TOC entry 3633 (class 2606 OID 57394)
-- Name: ratings uk_user_story_rating; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT uk_user_story_rating UNIQUE (user_id, story_id);


--
-- TOC entry 3896 (class 0 OID 0)
-- Dependencies: 3633
-- Name: CONSTRAINT uk_user_story_rating ON ratings; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON CONSTRAINT uk_user_story_rating ON public.ratings IS 'A user can only rate a story once (can be updated)';


--
-- TOC entry 3611 (class 2606 OID 32846)
-- Name: user_history_read user_history_read_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_history_read
    ADD CONSTRAINT user_history_read_pkey PRIMARY KEY (id);


--
-- TOC entry 3668 (class 2606 OID 163902)
-- Name: user_onboarding user_onboarding_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_onboarding
    ADD CONSTRAINT user_onboarding_pkey PRIMARY KEY (id);


--
-- TOC entry 3670 (class 2606 OID 163904)
-- Name: user_onboarding user_onboarding_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_onboarding
    ADD CONSTRAINT user_onboarding_user_id_key UNIQUE (user_id);


--
-- TOC entry 3662 (class 2606 OID 163875)
-- Name: user_profiles user_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (id);


--
-- TOC entry 3664 (class 2606 OID 163877)
-- Name: user_profiles user_profiles_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT user_profiles_user_id_key UNIQUE (user_id);


--
-- TOC entry 3588 (class 2606 OID 32799)
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- TOC entry 3590 (class 2606 OID 32797)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- TOC entry 3618 (class 1259 OID 32905)
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- TOC entry 3601 (class 1259 OID 32835)
-- Name: idx_chapter_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chapter_index ON public.story_chapters USING btree (story_id, chapter_index);


--
-- TOC entry 3602 (class 1259 OID 32834)
-- Name: idx_chapter_story; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chapter_story ON public.story_chapters USING btree (story_id);


--
-- TOC entry 3603 (class 1259 OID 81926)
-- Name: idx_chapters_updated_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_chapters_updated_at ON public.story_chapters USING btree (updated_at DESC);


--
-- TOC entry 3636 (class 1259 OID 57438)
-- Name: idx_comments_story; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_comments_story ON public.comments USING btree (story_id);


--
-- TOC entry 3637 (class 1259 OID 57439)
-- Name: idx_comments_story_created; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_comments_story_created ON public.comments USING btree (story_id, created_at DESC);


--
-- TOC entry 3638 (class 1259 OID 57437)
-- Name: idx_comments_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_comments_user ON public.comments USING btree (user_id);


--
-- TOC entry 3614 (class 1259 OID 32929)
-- Name: idx_crawl_jobs_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_crawl_jobs_status ON public.crawl_jobs USING btree (status);


--
-- TOC entry 3615 (class 1259 OID 32930)
-- Name: idx_crawl_jobs_story; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_crawl_jobs_story ON public.crawl_jobs USING btree (story_id);


--
-- TOC entry 3646 (class 1259 OID 73755)
-- Name: idx_favorites_created; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_favorites_created ON public.favorites USING btree (user_id, created_at DESC);


--
-- TOC entry 3647 (class 1259 OID 73754)
-- Name: idx_favorites_story; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_favorites_story ON public.favorites USING btree (story_id);


--
-- TOC entry 3648 (class 1259 OID 73753)
-- Name: idx_favorites_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_favorites_user ON public.favorites USING btree (user_id);


--
-- TOC entry 3606 (class 1259 OID 114692)
-- Name: idx_history_last_read; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_history_last_read ON public.user_history_read USING btree (last_read_at DESC);


--
-- TOC entry 3607 (class 1259 OID 114691)
-- Name: idx_history_user_story; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_history_user_story ON public.user_history_read USING btree (user_id, story_id);


--
-- TOC entry 3665 (class 1259 OID 163911)
-- Name: idx_onboarding_completed; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_onboarding_completed ON public.user_onboarding USING btree (completed);


--
-- TOC entry 3666 (class 1259 OID 163910)
-- Name: idx_onboarding_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_onboarding_user_id ON public.user_onboarding USING btree (user_id);


--
-- TOC entry 3651 (class 1259 OID 122905)
-- Name: idx_password_reset_tokens_expiry_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_password_reset_tokens_expiry_date ON public.password_reset_tokens USING btree (expiry_date);


--
-- TOC entry 3652 (class 1259 OID 122903)
-- Name: idx_password_reset_tokens_token; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_password_reset_tokens_token ON public.password_reset_tokens USING btree (token);


--
-- TOC entry 3653 (class 1259 OID 122904)
-- Name: idx_password_reset_tokens_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_password_reset_tokens_user_id ON public.password_reset_tokens USING btree (user_id);


--
-- TOC entry 3627 (class 1259 OID 57435)
-- Name: idx_ratings_story; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ratings_story ON public.ratings USING btree (story_id);


--
-- TOC entry 3628 (class 1259 OID 57436)
-- Name: idx_ratings_story_created; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ratings_story_created ON public.ratings USING btree (story_id, created_at DESC);


--
-- TOC entry 3629 (class 1259 OID 57434)
-- Name: idx_ratings_user; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_ratings_user ON public.ratings USING btree (user_id);


--
-- TOC entry 3639 (class 1259 OID 65571)
-- Name: idx_roles_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_roles_name ON public.roles USING btree (name);


--
-- TOC entry 3591 (class 1259 OID 32888)
-- Name: idx_stories_description; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_stories_description ON public.stories USING gin (to_tsvector('simple'::regconfig, description));


--
-- TOC entry 3592 (class 1259 OID 49162)
-- Name: idx_stories_embedding; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_stories_embedding ON public.stories USING ivfflat (embedding public.vector_cosine_ops);


--
-- TOC entry 3593 (class 1259 OID 81925)
-- Name: idx_stories_featured; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_stories_featured ON public.stories USING btree (featured) WHERE (featured = true);


--
-- TOC entry 3594 (class 1259 OID 98307)
-- Name: idx_stories_featured_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_stories_featured_status ON public.stories USING btree (featured, status);


--
-- TOC entry 3595 (class 1259 OID 98306)
-- Name: idx_stories_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_stories_status ON public.stories USING btree (status);


--
-- TOC entry 3596 (class 1259 OID 32907)
-- Name: idx_stories_title; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_stories_title ON public.stories USING gin (to_tsvector('simple'::regconfig, title));


--
-- TOC entry 3597 (class 1259 OID 81923)
-- Name: idx_stories_updated_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_stories_updated_at ON public.stories USING btree (updated_at DESC);


--
-- TOC entry 3598 (class 1259 OID 81924)
-- Name: idx_stories_view_count; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_stories_view_count ON public.stories USING btree (view_count DESC);


--
-- TOC entry 3623 (class 1259 OID 57433)
-- Name: idx_story_genres_genre; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_story_genres_genre ON public.story_genres USING btree (genre_id);


--
-- TOC entry 3624 (class 1259 OID 57432)
-- Name: idx_story_genres_story; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_story_genres_story ON public.story_genres USING btree (story_id);


--
-- TOC entry 3658 (class 1259 OID 163884)
-- Name: idx_user_profiles_embedding; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_profiles_embedding ON public.user_profiles USING ivfflat (profile_embedding public.vector_cosine_ops);


--
-- TOC entry 3659 (class 1259 OID 163885)
-- Name: idx_user_profiles_last_update; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_profiles_last_update ON public.user_profiles USING btree (last_profile_update);


--
-- TOC entry 3660 (class 1259 OID 163883)
-- Name: idx_user_profiles_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_profiles_user_id ON public.user_profiles USING btree (user_id);


--
-- TOC entry 3585 (class 1259 OID 65574)
-- Name: idx_users_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_active ON public.users USING btree (active);


--
-- TOC entry 3586 (class 1259 OID 131079)
-- Name: idx_users_role_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_users_role_id ON public.users USING btree (role_id);


--
-- TOC entry 3699 (class 2620 OID 163887)
-- Name: user_profiles trigger_update_user_profile_timestamp; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trigger_update_user_profile_timestamp BEFORE UPDATE ON public.user_profiles FOR EACH ROW EXECUTE FUNCTION public.update_user_profile_timestamp();


--
-- TOC entry 3684 (class 2606 OID 32882)
-- Name: crawl_jobs crawl_jobs_chapter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.crawl_jobs
    ADD CONSTRAINT crawl_jobs_chapter_id_fkey FOREIGN KEY (chapter_id) REFERENCES public.story_chapters(id) ON DELETE CASCADE;


--
-- TOC entry 3685 (class 2606 OID 32877)
-- Name: crawl_jobs crawl_jobs_story_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.crawl_jobs
    ADD CONSTRAINT crawl_jobs_story_id_fkey FOREIGN KEY (story_id) REFERENCES public.stories(id) ON DELETE CASCADE;


--
-- TOC entry 3674 (class 2606 OID 32909)
-- Name: story_chapters fk_chapter_story; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.story_chapters
    ADD CONSTRAINT fk_chapter_story FOREIGN KEY (story_id) REFERENCES public.stories(id) ON DELETE CASCADE;


--
-- TOC entry 3675 (class 2606 OID 106507)
-- Name: story_chapters fk_chapters_created_by; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.story_chapters
    ADD CONSTRAINT fk_chapters_created_by FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 3676 (class 2606 OID 106512)
-- Name: story_chapters fk_chapters_last_modified_by; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.story_chapters
    ADD CONSTRAINT fk_chapters_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 3692 (class 2606 OID 57427)
-- Name: comments fk_comment_story; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fk_comment_story FOREIGN KEY (story_id) REFERENCES public.stories(id) ON DELETE CASCADE;


--
-- TOC entry 3693 (class 2606 OID 57422)
-- Name: comments fk_comment_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3686 (class 2606 OID 106517)
-- Name: crawl_jobs fk_crawl_jobs_created_by; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.crawl_jobs
    ADD CONSTRAINT fk_crawl_jobs_created_by FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 3687 (class 2606 OID 106522)
-- Name: crawl_jobs fk_crawl_jobs_last_modified_by; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.crawl_jobs
    ADD CONSTRAINT fk_crawl_jobs_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 3694 (class 2606 OID 73748)
-- Name: favorites fk_favorite_story; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT fk_favorite_story FOREIGN KEY (story_id) REFERENCES public.stories(id) ON DELETE CASCADE;


--
-- TOC entry 3695 (class 2606 OID 73743)
-- Name: favorites fk_favorite_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.favorites
    ADD CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3678 (class 2606 OID 32924)
-- Name: user_history_read fk_history_chapter; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_history_read
    ADD CONSTRAINT fk_history_chapter FOREIGN KEY (chapter_id) REFERENCES public.story_chapters(id) ON DELETE SET NULL;


--
-- TOC entry 3679 (class 2606 OID 32919)
-- Name: user_history_read fk_history_story; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_history_read
    ADD CONSTRAINT fk_history_story FOREIGN KEY (story_id) REFERENCES public.stories(id) ON DELETE CASCADE;


--
-- TOC entry 3680 (class 2606 OID 32914)
-- Name: user_history_read fk_history_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_history_read
    ADD CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3698 (class 2606 OID 163905)
-- Name: user_onboarding fk_onboarding_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_onboarding
    ADD CONSTRAINT fk_onboarding_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3696 (class 2606 OID 122898)
-- Name: password_reset_tokens fk_password_reset_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3690 (class 2606 OID 57400)
-- Name: ratings fk_rating_story; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT fk_rating_story FOREIGN KEY (story_id) REFERENCES public.stories(id) ON DELETE CASCADE;


--
-- TOC entry 3691 (class 2606 OID 57395)
-- Name: ratings fk_rating_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ratings
    ADD CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3672 (class 2606 OID 106497)
-- Name: stories fk_stories_created_by; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.stories
    ADD CONSTRAINT fk_stories_created_by FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 3673 (class 2606 OID 106502)
-- Name: stories fk_stories_last_modified_by; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.stories
    ADD CONSTRAINT fk_stories_last_modified_by FOREIGN KEY (last_modified_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- TOC entry 3688 (class 2606 OID 57372)
-- Name: story_genres fk_story_genres_genre; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.story_genres
    ADD CONSTRAINT fk_story_genres_genre FOREIGN KEY (genre_id) REFERENCES public.genres(id) ON DELETE CASCADE;


--
-- TOC entry 3689 (class 2606 OID 57367)
-- Name: story_genres fk_story_genres_story; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.story_genres
    ADD CONSTRAINT fk_story_genres_story FOREIGN KEY (story_id) REFERENCES public.stories(id) ON DELETE CASCADE;


--
-- TOC entry 3697 (class 2606 OID 163878)
-- Name: user_profiles fk_user_profiles_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_profiles
    ADD CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- TOC entry 3671 (class 2606 OID 131074)
-- Name: users fk_users_role; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES public.roles(id);


--
-- TOC entry 3677 (class 2606 OID 32829)
-- Name: story_chapters story_chapters_story_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.story_chapters
    ADD CONSTRAINT story_chapters_story_id_fkey FOREIGN KEY (story_id) REFERENCES public.stories(id) ON DELETE CASCADE;


--
-- TOC entry 3681 (class 2606 OID 32857)
-- Name: user_history_read user_history_read_chapter_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_history_read
    ADD CONSTRAINT user_history_read_chapter_id_fkey FOREIGN KEY (chapter_id) REFERENCES public.story_chapters(id) ON DELETE CASCADE;


--
-- TOC entry 3682 (class 2606 OID 32852)
-- Name: user_history_read user_history_read_story_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_history_read
    ADD CONSTRAINT user_history_read_story_id_fkey FOREIGN KEY (story_id) REFERENCES public.stories(id) ON DELETE CASCADE;


--
-- TOC entry 3683 (class 2606 OID 32847)
-- Name: user_history_read user_history_read_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_history_read
    ADD CONSTRAINT user_history_read_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


-- Completed on 2026-01-29 22:42:53

--
-- PostgreSQL database dump complete
--

\unrestrict ccwRqpVhAR7eS8iYdVTU1ppZrqLYnZFlgcoe5HyS6CYmfXYBYMFgHhq5IshD3kY

