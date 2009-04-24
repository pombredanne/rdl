Om exempelapplikationen
-----------------------

Syftet med denna exempelapplikation �r att illustrera i programkod hur man kan
implementera de format som rekommenderas av R�ttsinformationsprojektet f�r den
mest grundl�ggande niv�n. Tanken �r inte att tillhandah�lla ett f�rdigt system
f�r r�ttsinformationshantering.

Applikationem �r byggd p� webbramverket Django som ges ut under BSD-licensen.
Se BSD-licensen f�r mer information om m�jligheter att anv�nda dig av
programkoden. Domstolsverket ger inga garantier f�r dess funktion eller
l�mplighet och fr�ns�ger sig ansvar f�r eventuella fel och brister.

Vi �r dock tacksamma f�r feedback och rapporter om eventuella fel.

Installationsansvisningar
-------------------------

Rader som b�rjar med "$" avser kommandon som skall utf�ras fr�n ett
terminalf�nster).

1. Installera programspr�ket Python 2.6 (Se http://www.python.org/download/).

2. Installera easy_install/setuptools (Se
http://pypi.python.org/pypi/setuptools och
http://peak.telecommunity.com/DevCenter/EasyInstall)

3. Installera Django
    $ easy_install django

4. Installera Sqllite 3 (Se http://www.sqlite.org/)

6. Redigera settings.py och ange:
    DATABASE_NAME    S�kv�g till databasfilen
    Modifiera vid behov de inst�llningar som b�rjar med RINFO

7. Installera databasschemat
    $ python manage.py syncdb

8. Starta den inbyggda webbservern
    $ python manage.py runserver

9. �ppna webbl�saren med f�ljande adress: http://127.0.0.1:8000/
    Exempelwebbplatsen visas. F�r att redigera inneh�ll navigera till
http://127.0.0.1/admin/ och logga in som anv�ndare "admin" med l�senord
"admin".



