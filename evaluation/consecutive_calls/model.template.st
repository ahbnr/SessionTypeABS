
0 -f-> P:m.
(
  {% for method in methods %}
  {% if method in indirection_methods %}
  P -f{{method}}-> Indirection:{{method}}.
    {% if use_await %}
    Rel(P, f{{method}}).
    {% endif %}
    Indirection -fI{{method}}-> Q:{{method}}.
      {% if use_await %}
      Rel(Indirection, fI{{method}}).
      {% endif %}
      Q resolves fI{{method}}.
    Indirection resolves f{{method}}{% if loop.nextitem is defined %}.{% endif %}
  {% else %}
  P -f{{method}}-> Q:{{method}}.
    {% if use_await %}
    Rel(P, f{{method}}).
    {% endif %}
    Q resolves f{{method}}{% if loop.nextitem is defined %}.{% endif %}
  {% endif %}
  {% endfor %}
)*.
P resolves f

