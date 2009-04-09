
Installationsansvisningar
-------------------------

Rader som b�rjar med "$" avser kommandon som skall utf�ras fr�n ett terminalf�nster).

1. Installera programspr�ket Python

2. Installera easy_install

3. Installera Django

4. Installera Sqllite 3

5. Installera applikationen

6. Redigera settings.py och ange:
    1. DATABASE_NAME    S�kv�g till databasfilen
    2. RINFO_ORG_URI    Identifieraren (i URI-format) f�r din organisation (erh�lls fr�n projektet)

7. Installera databasschemat
    $ python manage.py syncdb

8. Starta den inbyggda webbservern
    $ python manage.py runserver

9. �ppna webbl�saren med f�ljande adress: http://127.0.0.1:8000/admin f�r att redigera inneh�ll. Logga in som admin/admin.
