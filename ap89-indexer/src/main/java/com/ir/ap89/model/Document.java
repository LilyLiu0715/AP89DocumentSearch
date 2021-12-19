package com.ir.ap89.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A class that represents an AP89 document.
 */
public class Document {
    // XML constants.
    public static final String XML_DOCNO_TAG_NAME = "DOCNO";
    public static final String XML_FILEID_TAG_NAME = "FILEID";
    public static final String XML_NOTE_TAG_NAME = "NOTE";
    public static final String XML_UNK_TAG_NAME = "UNK";
    public static final String XML_FIRST_TAG_NAME = "FIRST";
    public static final String XML_SECOND_TAG_NAME = "SECOND";
    public static final String XML_HEAD_TAG_NAME = "HEAD";
    public static final String XML_DATELINE_TAG_NAME = "DATELINE";
    public static final String XML_TEXT_TAG_NAME = "TEXT";
    public static final String XML_BYLINE_TAG_NAME = "BYLINE";

    // JSON constants.
    public static final String DOCNO_FIELD_NAME = "DocNo";
    public static final String FILEID_FIELD_NAME = "FileId";
    public static final String NOTE_FIELD_NAME = "Note";
    public static final String UNK_FIELD_NAME = "Unk";
    public static final String FIRST_FIELD_NAME = "First";
    public static final String SECOND_FIELD_NAME = "Second";
    public static final String HEAD_FIELD_NAME = "Head";
    public static final String DATELINE_FIELD_NAME = "Dateline";
    public static final String TEXT_FIELD_NAME = "Text";
    public static final String BYLINE_FIELD_NAME = "Byline";

    public static final List<String> QUERIABLE_FIELD_NAMES = Arrays.asList(
        NOTE_FIELD_NAME, UNK_FIELD_NAME, FIRST_FIELD_NAME, SECOND_FIELD_NAME, 
        HEAD_FIELD_NAME, DATELINE_FIELD_NAME, TEXT_FIELD_NAME, BYLINE_FIELD_NAME
    );

    public static final List<String> LESS_NOISY_FIELD_NAMES = Arrays.asList(
        DATELINE_FIELD_NAME, TEXT_FIELD_NAME, BYLINE_FIELD_NAME
    );
    
    @JsonProperty(DOCNO_FIELD_NAME)
    public String docNo;

    @JsonProperty(FILEID_FIELD_NAME)
    public String fileId;

    @JsonProperty(NOTE_FIELD_NAME)
    public String note;

    @JsonProperty(UNK_FIELD_NAME)
    public String unk;

    @JsonProperty(FIRST_FIELD_NAME)
    public String first;

    @JsonProperty(SECOND_FIELD_NAME)
    public String second;

    @JsonProperty(HEAD_FIELD_NAME)
    @JsonDeserialize(as = ArrayList.class, contentAs = String.class)
    public List<String> head = new ArrayList<>();

    @JsonProperty(DATELINE_FIELD_NAME)
    public String dateline;

    @JsonProperty(TEXT_FIELD_NAME)
    @JsonDeserialize(as = ArrayList.class, contentAs = String.class)
    public ArrayList<String> text = new ArrayList<>();

    @JsonProperty(BYLINE_FIELD_NAME)
    @JsonDeserialize(as = ArrayList.class, contentAs = String.class)
    public ArrayList<String> byline = new ArrayList<>();

    public Document() {}

    @JsonCreator
    public Document(@JsonProperty(DOCNO_FIELD_NAME) String docNo,
                    @JsonProperty(FILEID_FIELD_NAME) String fileId,
                    @JsonProperty(NOTE_FIELD_NAME) String note,
                    @JsonProperty(UNK_FIELD_NAME) String unk,
                    @JsonProperty(FIRST_FIELD_NAME) String first,
                    @JsonProperty(SECOND_FIELD_NAME) String second,
                    @JsonProperty(HEAD_FIELD_NAME) List<String> head,
                    @JsonProperty(DATELINE_FIELD_NAME) String dateline,
                    @JsonProperty(TEXT_FIELD_NAME) ArrayList<String> text,
                    @JsonProperty(BYLINE_FIELD_NAME) ArrayList<String> byline) {
        this.docNo = docNo;
        this.fileId = fileId;
        this.note = note;
        this.unk = unk;
        this.first = first;
        this.second = second;
        this.head = head;
        this.dateline = dateline;
        this.text = text;
        this.byline = byline;
    }

    public static Document parseFromXML(Element doc) {
        Document document = new Document();

        document.docNo = doc.getElementsByTagName(XML_DOCNO_TAG_NAME)
            .item(0).getTextContent();

        document.fileId = doc.getElementsByTagName(XML_FILEID_TAG_NAME)
            .item(0).getTextContent();

        NodeList noteList = doc.getElementsByTagName(XML_NOTE_TAG_NAME);
        if (noteList != null && noteList.getLength() > 0) {
            document.note = noteList.item(0).getTextContent();
        }

        NodeList unkList = doc.getElementsByTagName(XML_UNK_TAG_NAME);
        if (unkList != null && unkList.getLength() > 0) {
            document.unk = unkList.item(0).getTextContent();
        }

        NodeList firstList = doc.getElementsByTagName(XML_FIRST_TAG_NAME);
        if (firstList != null && firstList.getLength() > 0) {
            document.first  = firstList.item(0).getTextContent();
        }

        NodeList secondList = doc.getElementsByTagName(XML_SECOND_TAG_NAME);
        if (secondList != null && secondList.getLength() > 0) {
            document.second = secondList.item(0).getTextContent();
        }

        NodeList headList = doc.getElementsByTagName(XML_HEAD_TAG_NAME);
        if (headList != null) {
            for (int i = 0; i < headList.getLength(); i++) {
                document.head.add(headList.item(i).getTextContent());
            }
        }

        NodeList datelineList = doc.getElementsByTagName(XML_DATELINE_TAG_NAME);
        if (datelineList != null && datelineList.getLength() > 0) {
            document.dateline = datelineList.item(0).getTextContent();
        }

        NodeList textList = doc.getElementsByTagName(XML_TEXT_TAG_NAME);
        if (textList != null) {
            for (int i = 0; i < textList.getLength(); i++) {
                document.text.add(textList.item(i).getTextContent());
           }
        }

        NodeList bylineList = doc.getElementsByTagName(XML_BYLINE_TAG_NAME);
        if (bylineList != null) {
            for (int i = 0; i < bylineList.getLength(); i++) {
                document.byline.add(bylineList.item(i).getTextContent());
            }
        }

        return document;
    }
}

