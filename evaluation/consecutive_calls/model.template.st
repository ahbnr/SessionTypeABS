
0 -f-> P:m.
(
  {% for method in methods %}
  {% if method in indirection_methods %}
  P -f{{method}}-> Indirection:{{method}}.
    Indirection -fI{{method}}-> Q:{{method}}.
      Q resolves fI{{method}}.
    Indirection resolves f{{method}}{% if loop.nextitem is defined %}.{% endif %}
  {% else %}
  P -f{{method}}-> Q:{{method}}.
    Q resolves f{{method}}{% if loop.nextitem is defined %}.{% endif %}
  {% endif %}
  {% endfor %}
)*.
P resolves f

