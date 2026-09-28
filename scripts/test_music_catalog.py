import unittest
from prepare_music_catalog import similarity, BAD, candidates, duration

class MusicMatchingTest(unittest.TestCase):
    def test_hindi_names_do_not_match_unrelated_songs(self):
        self.assertEqual(0,similarity('मंगल मूर्ति मारुति नंदन','अल्लाह तेरो नाम ईश्वर तेरो नाम'))
        self.assertEqual(0,similarity('कीजो केसरी के लाल','हनुमान चालीसा जय श्री हनुमान जी की'))
    def test_repeated_deity_words_cannot_match_generic_album_tags(self):
        self.assertEqual(0,similarity('Main Shiv Ka Shiv Mere',
            'Bum Bum Bum Mere Bhole Bhandari.mp3 Bum Bum Bum Mere Bhole Bhandari Shiv Evergreen Bhajan'))
    def test_transliteration_and_hindi(self):
        self.assertGreater(similarity('Shiv Shankar Ko Jisne Pooja','Shiv Shanker Ko Jisne Puja.mp3'),.86)
        self.assertEqual(1,similarity('हनुमान चालीसा','हनुमान चालीसा.mp3'))
    def test_altered_sources_excluded(self):
        for label in ['Shiv Chalisa Super Fast','Mera Bhola 8D audio','Hanuman remix','Bajrang Baan 7 times']:
            self.assertIsNotNone(BAD.search(label))
        self.assertEqual([],candidates({'title':'Shiv Chalisa'},[({'title':'Shiv Chalisa Super Fast'},{'name':'Shiv Chalisa.mp3'})]))
    def test_duration_formats(self):
        self.assertEqual(125,duration('02:05'))
        self.assertEqual(62.5,duration('62.5'))
        self.assertEqual(0,duration(None))

if __name__=='__main__':unittest.main()
