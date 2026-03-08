--
-- PostgreSQL database dump
--

\restrict 8DdwIjd3KeTeaAnhsxBfxiCrl2PqJYhuFRazHEVbykSDcV8RCmn0F3sS5nbAHgM

-- Dumped from database version 18.1
-- Dumped by pg_dump version 18.1

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: request_details; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.request_details (
    request_id character varying(255) NOT NULL,
    url character varying(255) NOT NULL,
    request_type character varying(255) NOT NULL,
    video_quality character varying(255),
    video_format character varying(255),
    audio_quality character varying(255),
    audio_format character varying(255),
    metadata boolean NOT NULL,
    version bigint,
    CONSTRAINT valid_audio_format CHECK (((audio_format)::text = ANY (ARRAY[('default'::character varying)::text, ('mp3'::character varying)::text, ('m4a'::character varying)::text, ('flac'::character varying)::text]))),
    CONSTRAINT valid_audio_quality CHECK (((audio_quality)::text = ANY (ARRAY[('worst'::character varying)::text, ('128kbps'::character varying)::text, ('160kbps'::character varying)::text, ('192kbps'::character varying)::text, ('256kbps'::character varying)::text, ('320kbps'::character varying)::text, ('flac'::character varying)::text, ('best'::character varying)::text]))),
    CONSTRAINT valid_request_type CHECK (((request_type)::text = ANY (ARRAY[('video'::character varying)::text, ('video_only'::character varying)::text, ('audio_only'::character varying)::text]))),
    CONSTRAINT valid_video_format CHECK (((video_format)::text = ANY (ARRAY[('default'::character varying)::text, ('mp4'::character varying)::text, ('mkv'::character varying)::text]))),
    CONSTRAINT valid_video_quality CHECK (((video_quality)::text = ANY ((ARRAY['worst'::character varying, '144p'::character varying, '240p'::character varying, '360p'::character varying, '480p'::character varying, '720p'::character varying, '1080p'::character varying, '1440p'::character varying, '2160p'::character varying, 'best'::character varying])::text[])))
);


--
-- Name: requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.requests (
    id character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completed_at timestamp with time zone,
    version bigint,
    CONSTRAINT valid_status CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'ongoing'::character varying, 'completed'::character varying, 'failed'::character varying, 'cancelled'::character varying])::text[])))
);


--
-- Name: resources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.resources (
    request_id character varying(255) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    expire_at timestamp with time zone,
    storage_used bigint NOT NULL,
    fetch_count integer,
    deleted_at timestamp with time zone,
    version bigint,
    CONSTRAINT is_valid CHECK (((expire_at IS NULL) OR (created_at < expire_at))),
    CONSTRAINT valid_fetch_count CHECK ((fetch_count >= 0))
);


--
-- Name: request_details request_details_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.request_details
    ADD CONSTRAINT request_details_pkey PRIMARY KEY (request_id);


--
-- Name: requests requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT requests_pkey PRIMARY KEY (id);


--
-- Name: resources resources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT resources_pkey PRIMARY KEY (request_id);


--
-- Name: request_details request_details_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.request_details
    ADD CONSTRAINT request_details_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.requests(id);


--
-- Name: resources resources_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.resources
    ADD CONSTRAINT resources_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.requests(id);


--
-- PostgreSQL database dump complete
--

\unrestrict 8DdwIjd3KeTeaAnhsxBfxiCrl2PqJYhuFRazHEVbykSDcV8RCmn0F3sS5nbAHgM

